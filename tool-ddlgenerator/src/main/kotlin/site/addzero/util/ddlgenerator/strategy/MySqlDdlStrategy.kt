package site.addzero.util.ddlgenerator.strategy

import site.addzero.ioc.annotation.Component
import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerationStrategy
import site.addzero.util.ddlgenerator.assist.defaultSimpleTypeMappings
import site.addzero.util.ddlgenerator.assist.mapStringType
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.database.model.ForeignKeyInfo
import site.addzero.util.lsi.field.LsiField
import site.addzero.util.lsi_impl.impl.database.clazz.getAllDbFields
import site.addzero.util.lsi_impl.impl.database.clazz.getDatabaseForeignKeys
import site.addzero.util.lsi_impl.impl.database.field.*

/**
 * MySQL方言的DDL生成策略
 */
@Component
class MySqlDdlStrategy : DdlGenerationStrategy {
    override val simpleTypeMappings: Map<String, (LsiField) -> String>
        get() {
            defaultSimpleTypeMappings.putAll(
                mapOf(
                    // Kotlin类型
                    "Int" to { "INT" },
                    "Long" to { "BIGINT" },
                    "Short" to { "SMALLINT" },
                    "Byte" to { "TINYINT" },
                    "Float" to { "FLOAT" },
                    "Double" to { "DOUBLE" },
                    "String" to { field -> mapStringType(field) },
                    "Char" to { "CHAR(1)" },
                    "Boolean" to { "TINYINT(1)" },
                    // Kotlin日期时间（kotlinx-datetime）
                    "LocalDate" to { "DATE" },
                    "LocalTime" to { "TIME" },
                    "LocalDateTime" to { "DATETIME" },
                    "Instant" to { "TIMESTAMP" }
                ))
            return defaultSimpleTypeMappings
        }


    override fun supports(dialect: DatabaseType): Boolean {
        return dialect == DatabaseType.MYSQL
    }

    override fun generateCreateTable(lsiClass: LsiClass): String {
        val tableName = lsiClass.guessTableName
        val columns = lsiClass.getAllDbFields()

        val columnsSql = columns.joinToString(",\n  ") { field ->
            buildColumnDefinition(field)
        }
        // 查找自增主键列以设置AUTO_INCREMENT选项
        val autoIncrementOption = columns.find { it.isAutoIncrement }?.let {
            " AUTO_INCREMENT=1"
        } ?: ""

        return """
            |CREATE TABLE `$tableName` (
            |  $columnsSql
            |)$autoIncrementOption;
            """.trimMargin()
    }

    override fun generateDropTable(tableName: String): String {
        return "DROP TABLE IF EXISTS `$tableName`;"
    }

    override fun generateAddColumn(tableName: String, field: LsiField): String {
        val columnDefinition = buildColumnDefinition(field)
        return "ALTER TABLE `$tableName` ADD COLUMN $columnDefinition;"
    }

    override fun generateDropColumn(tableName: String, columnName: String): String {
        return "ALTER TABLE `$tableName` DROP COLUMN `$columnName`;"
    }

    override fun generateModifyColumn(tableName: String, field: LsiField): String {
        val columnDefinition = buildColumnDefinition(field)
        return "ALTER TABLE `$tableName` MODIFY COLUMN $columnDefinition;"
    }

    override fun generateAddForeignKey(tableName: String, foreignKey: ForeignKeyInfo): String {
        return "ALTER TABLE `$tableName` ADD CONSTRAINT `${foreignKey.name}` FOREIGN KEY (`${foreignKey.columnName}`) REFERENCES `${foreignKey.referencedTable}` (`${foreignKey.referencedColumn}`);"
    }

    override fun generateAddComment(lsiClass: LsiClass): String {
        val statements = mutableListOf<String>()
        val tableName = lsiClass.guessTableName

        // 表注释
        if (lsiClass.comment != null) {
            statements.add("ALTER TABLE `$tableName` COMMENT='${lsiClass.comment}';")
        }

        // 列注释
        lsiClass.getAllDbFields().filter { it.comment != null }.forEach { field ->
            val columnName = field.columnName ?: field.name ?: return@forEach
            val columnType = mapFieldToColumnType(field)
            statements.add("ALTER TABLE `$tableName` MODIFY `$columnName` $columnType COMMENT '${field.comment}';")
        }

        return statements.joinToString("\n")
    }

    override fun generateAll(lsiClasses: List<LsiClass>): String {
        // MySQL支持在CREATE TABLE语句中定义外键，所以可以直接按顺序创建表
        val createTableStatements = lsiClasses.map { lsiClass -> generateCreateTable(lsiClass) }
        val addConstraintsStatements = lsiClasses.flatMap { lsiClass ->
            val foreignKeyStatements = lsiClass.getDatabaseForeignKeys().map { fk ->
                generateAddForeignKey(lsiClass.guessTableName, fk)
            }
            val commentStatements =
                if (lsiClass.comment != null || lsiClass.getAllDbFields().any { it.comment != null }) {
                    listOf(generateAddComment(lsiClass))
                } else {
                    emptyList()
                }
            foreignKeyStatements + commentStatements
        }

        return (createTableStatements + addConstraintsStatements).joinToString("\n\n")
    }

    private fun buildColumnDefinition(field: LsiField): String {
        val builder = StringBuilder()
        val columnName = field.columnName ?: field.name ?: "unknown"

        // 使用新的类型映射方法
        val columnTypeName = mapFieldToColumnType(field)

        builder.append("`$columnName` $columnTypeName")

        if (!field.isNullable) {
            builder.append(" NOT NULL")
        }

        if (field.isAutoIncrement) {
            builder.append(" AUTO_INCREMENT")
        }

        if (field.defaultValue != null) {
            builder.append(" DEFAULT ${field.defaultValue}")
        }

        if (field.isPrimaryKey) {
            builder.append(" PRIMARY KEY")
        }

        return builder.toString()
    }

    /**
     * 将字段类型映射到数据库列类型
     */
    private fun mapFieldToColumnType(field: LsiField): String {
        val typeName = field.typeName ?: return "TEXT"
        return simpleTypeMappings[typeName]?.invoke(field) ?: "TEXT"
    }

}
