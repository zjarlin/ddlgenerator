package site.addzero.util.ddlgenerator.strategy

import site.addzero.ioc.annotation.Component
import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerationStrategy
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.database.model.DatabaseColumnType
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
    override val simpleTypeMappings: Map<String, (LsiField) -> String> = mapOf(
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
    )

    override fun getDatabaseSpecificType(field: LsiField): String? {
        // 检查@Column(columnDefinition)
        field.annotations.firstOrNull {
            it.qualifiedName?.endsWith("Column") == true
        }?.let { columnAnno ->
            columnAnno.getAttribute("columnDefinition")?.toString()?.let { def ->
                if (def.isNotBlank() && def != "null") {
                    return def
                }
            }
        }

        return null
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
            val columnType = field.getDatabaseColumnType()
            statements.add("ALTER TABLE `$tableName` MODIFY `$columnName` ${getColumnTypeName(columnType)} COMMENT '${field.comment}';")
        }

        return statements.joinToString("\n")
    }

    override fun generateSchema(lsiClasses: List<LsiClass>): String {
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

    override fun getColumnTypeName(columnType: DatabaseColumnType, precision: Int?, scale: Int?): String {
        return when (columnType) {
            DatabaseColumnType.INT -> "INT"
            DatabaseColumnType.BIGINT -> "BIGINT"
            DatabaseColumnType.SMALLINT -> "SMALLINT"
            DatabaseColumnType.TINYINT -> "TINYINT"
            DatabaseColumnType.DECIMAL -> {
                if (precision != null && scale != null) {
                    "DECIMAL($precision, $scale)"
                } else if (precision != null) {
                    "DECIMAL($precision)"
                } else {
                    "DECIMAL"
                }
            }

            DatabaseColumnType.FLOAT -> "FLOAT"
            DatabaseColumnType.DOUBLE -> "DOUBLE"
            DatabaseColumnType.VARCHAR -> {
                if (precision != null) {
                    "VARCHAR($precision)"
                } else {
                    "VARCHAR(255)"
                }
            }

            DatabaseColumnType.CHAR -> {
                if (precision != null) {
                    "CHAR($precision)"
                } else {
                    "CHAR(255)"
                }
            }

            DatabaseColumnType.TEXT -> "TEXT"
            DatabaseColumnType.LONGTEXT -> "LONGTEXT"
            DatabaseColumnType.DATE -> "DATE"
            DatabaseColumnType.TIME -> "TIME"
            DatabaseColumnType.DATETIME -> "DATETIME"
            DatabaseColumnType.TIMESTAMP -> "TIMESTAMP"
            DatabaseColumnType.BOOLEAN -> "TINYINT(1)"
            DatabaseColumnType.BLOB -> "BLOB"
            DatabaseColumnType.BYTES -> "BLOB"
        }
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
     * MySQL字符串类型映射
     *
     * 根据长度智能选择：
     * - <= 255: VARCHAR(n)
     * - 256 - 65,535: VARCHAR(n) 或 TEXT
     * - 65,536 - 16,777,215: MEDIUMTEXT
     * - > 16,777,215: LONGTEXT
     */
    private fun mapStringType(field: LsiField): String {
        // 1. 检查是否为长文本
        if (field.isText) {
            val length = field.length
            return when {
                length > 16_777_215 -> "LONGTEXT"
                length > 65_535 -> "MEDIUMTEXT"
                else -> "TEXT"
            }
        }

        // 2. 普通字符串
        val length = field.length
        return when {
            length > 0 -> "VARCHAR($length)"
            else -> "VARCHAR(255)" // 默认长度
        }
    }
}
