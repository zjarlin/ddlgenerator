package site.addzero.util.ddlgenerator.strategy

import org.babyfish.jimmer.config.autoddl.Settings
import site.addzero.ioc.annotation.Component
import site.addzero.util.db.DatabaseType
import site.addzero.util.lsi.database.dialect.DdlGenerationStrategy
import site.addzero.util.ddlgenerator.assist.getNativeTypeString
import site.addzero.util.ddlgenerator.config.databaseType
import site.addzero.util.ddlgenerator.config.strategy
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.database.model.ForeignKeyInfo
import site.addzero.util.lsi.field.LsiField
import site.addzero.util.lsi_impl.impl.database.clazz.getAllDbFields
import site.addzero.util.lsi_impl.impl.database.field.getDatabaseColumnType
import site.addzero.util.lsi_impl.impl.database.field.isAutoIncrement
import site.addzero.util.lsi_impl.impl.database.field.isPrimaryKey

/**
 * MySQL方言的DDL生成策略
 */
@Single
class MySqlDdlStrategy : DdlGenerationStrategy {

    private val dialect by lazy { DatabaseDialects.fromDatabaseName(DatabaseType.MYSQL.name.lowercase()) }
    private val quote by lazy { dialect.quoteIdentifier() }

    override fun databaseType() = DatabaseType.MYSQL

    override fun supports(): Boolean {
        return Settings.databaseType == DatabaseType.MYSQL
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
            |CREATE TABLE $quote$tableName$quote (
            |  $columnsSql
            |)$autoIncrementOption;
            """.trimMargin()
    }

    override fun generateDropTable(tableName: String): String {
        return "DROP TABLE IF EXISTS $quote$tableName$quote;"
    }

    override fun generateAddColumn(tableName: String, field: LsiField): String {
        val columnDefinition = buildColumnDefinition(field)
        return "ALTER TABLE $quote$tableName$quote ADD COLUMN $columnDefinition;"
    }

    override fun generateDropColumn(tableName: String, columnName: String): String {
        return "ALTER TABLE $quote$tableName$quote DROP COLUMN $quote$columnName$quote;"
    }

    override fun generateModifyColumn(tableName: String, field: LsiField): String {
        val columnDefinition = buildColumnDefinition(field)
        return "ALTER TABLE $quote$tableName$quote MODIFY COLUMN $columnDefinition;"
    }

    override fun generateAddForeignKey(tableName: String, foreignKey: ForeignKeyInfo): String {
        return "ALTER TABLE $quote$tableName$quote ADD CONSTRAINT $quote${foreignKey.name}$quote FOREIGN KEY ($quote${foreignKey.columnName}$quote) REFERENCES $quote${foreignKey.referencedTableName}$quote ($quote${foreignKey.referencedColumnName}$quote);"
    }

    override fun generateAddComment(lsiClass: LsiClass): String {
        val statements = mutableListOf<String>()
        val tableName = lsiClass.guessTableName

        // 表注释
        if (lsiClass.comment != null) {
            statements.add("ALTER TABLE $quote$tableName$quote COMMENT='${lsiClass.comment}';")
        }

        // 列注释
        lsiClass.getAllDbFields().filter { it.comment != null }.forEach { field ->
            val columnName = field.columnName ?: field.name ?: return@forEach
            val columnType = field.getTypeString()
            statements.add("ALTER TABLE $quote$tableName$quote MODIFY $quote$columnName$quote $columnType COMMENT '${field.comment}';")
        }

        return statements.joinToString("\n")
    }

    private fun buildColumnDefinition(field: LsiField): String {
        val strategy = Settings.strategy


        val builder = StringBuilder()
        val columnName = field.columnName ?: field.name ?: "unknown"
        // 使用方言适配器获取类型字符串
        val columnTypeName = field.getDatabaseColumnType()

        builder.append("$quote$columnName$quote $columnTypeName")

        if (!field.isNullable) {
            builder.append(" NOT NULL")
        }

        if (field.isAutoIncrement) {
            builder.append(" ").append(dialect.getAutoIncrementSyntax())
        }

        if (field.defaultValue != null) {
            builder.append(" DEFAULT ${field.defaultValue}")
        }

        if (field.isPrimaryKey) {
            builder.append(" PRIMARY KEY")
        }

        return builder.toString()
    }
}
