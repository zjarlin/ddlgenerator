package site.addzero.util.ddlgenerator.strategy

import site.addzero.ioc.annotation.Component
import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerationStrategy
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.database.*
import site.addzero.util.lsi.database.model.DatabaseColumnType
import site.addzero.util.lsi.database.model.ForeignKeyInfo
import site.addzero.util.lsi.field.LsiField
import site.addzero.util.lsi_impl.impl.database.clazz.getAllDbFields
import site.addzero.util.lsi_impl.impl.database.clazz.getDatabaseForeignKeys
import site.addzero.util.lsi_impl.impl.database.field.*

/**
 * PostgreSQL方言的DDL生成策略
 */
@Component
class PostgreSqlDdlStrategy : DdlGenerationStrategy {

    override val typeMappings: Map<String, (LsiField) -> String> = mapOf(
        // 整数类型
        "java.lang.Integer" to { "INTEGER" },
        "java.lang.Long" to { "BIGINT" },
        "java.lang.Short" to { "SMALLINT" },
        "java.lang.Byte" to { "SMALLINT" }, // PostgreSQL没有TINYINT

        // 浮点类型
        "java.lang.Float" to { "REAL" },
        "java.lang.Double" to { "DOUBLE PRECISION" },
        "java.math.BigDecimal" to { field ->
            val precision = field.precision
            val scale = field.scale
            when {
                precision > 0 && scale > 0 -> "NUMERIC($precision, $scale)"
                precision > 0 -> "NUMERIC($precision)"
                else -> "NUMERIC(19, 2)"
            }
        },
        "java.math.BigInteger" to { "NUMERIC(65, 0)" },

        // 字符串类型
        "java.lang.String" to { field -> mapStringType(field) },

        // 字符类型
        "java.lang.Character" to { "CHAR(1)" },

        // 布尔类型
        "java.lang.Boolean" to { "BOOLEAN" },

        // 日期时间类型
        "java.util.Date" to { "TIMESTAMP" },
        "java.sql.Date" to { "DATE" },
        "java.sql.Time" to { "TIME" },
        "java.sql.Timestamp" to { "TIMESTAMP" },
        "java.time.LocalDate" to { "DATE" },
        "java.time.LocalTime" to { "TIME" },
        "java.time.LocalDateTime" to { "TIMESTAMP" },
        "java.time.ZonedDateTime" to { "TIMESTAMP WITH TIME ZONE" },
        "java.time.OffsetDateTime" to { "TIMESTAMP WITH TIME ZONE" },
        "java.time.Instant" to { "TIMESTAMP WITH TIME ZONE" },
        "java.time.Duration" to { "INTERVAL" },

        // 二进制类型
        "byte[]" to { "BYTEA" },
        "[B" to { "BYTEA" },

        // UUID类型（PostgreSQL原生支持）
        "java.util.UUID" to { "UUID" },

        // JSON类型
        "org.babyfish.jimmer.sql.JsonNode" to { "JSONB" },
        "com.fasterxml.jackson.databind.JsonNode" to { "JSONB" },

        // 数组类型（PostgreSQL特有）
        "java.lang.Integer[]" to { "INTEGER[]" },
        "java.lang.Long[]" to { "BIGINT[]" },
        "java.lang.String[]" to { "TEXT[]" },
        "[Ljava.lang.Integer;" to { "INTEGER[]" },
        "[Ljava.lang.Long;" to { "BIGINT[]" },
        "[Ljava.lang.String;" to { "TEXT[]" }
    )

    override val simpleTypeMappings: Map<String, (LsiField) -> String> = mapOf(
        // Kotlin类型
        "Int" to { "INTEGER" },
        "Long" to { "BIGINT" },
        "Short" to { "SMALLINT" },
        "Byte" to { "SMALLINT" },
        "Float" to { "REAL" },
        "Double" to { "DOUBLE PRECISION" },
        "String" to { field -> mapStringType(field) },
        "Char" to { "CHAR(1)" },
        "Boolean" to { "BOOLEAN" },

        // Kotlin日期时间
        "LocalDate" to { "DATE" },
        "LocalTime" to { "TIME" },
        "LocalDateTime" to { "TIMESTAMP" },
        "Instant" to { "TIMESTAMP WITH TIME ZONE" },
        "Duration" to { "INTERVAL" },

        // Kotlin数组
        "IntArray" to { "INTEGER[]" },
        "LongArray" to { "BIGINT[]" },
        "Array<String>" to { "TEXT[]" }
    )


    override fun supports(dialect: DatabaseType): Boolean {
        return dialect == DatabaseType.POSTGRESQL
    }

    override fun generateCreateTable(lsiClass: LsiClass): String {
        val tableName = lsiClass.guessTableName
        val columns = lsiClass.getAllDbFields()

        val columnsSql = columns.joinToString(",\n  ") { field ->
            buildColumnDefinition(field)
        }

        return """
            |CREATE TABLE "$tableName" (
            |  $columnsSql
            |);
            """.trimMargin()
    }

    override fun generateDropTable(tableName: String): String {
        return "DROP TABLE IF EXISTS \"$tableName\";"
    }

    override fun generateAddColumn(tableName: String, field: LsiField): String {
        val columnDefinition = buildColumnDefinition(field)
        return "ALTER TABLE \"$tableName\" ADD COLUMN $columnDefinition;"
    }

    override fun generateDropColumn(tableName: String, columnName: String): String {
        return "ALTER TABLE \"$tableName\" DROP COLUMN \"$columnName\";"
    }

    override fun generateModifyColumn(tableName: String, field: LsiField): String {
        val columnName = field.columnName ?: field.name ?: "unknown"
        val columnType = field.getDatabaseColumnType()
        val statements = mutableListOf<String>()

        // PostgreSQL需要分别修改类型、可空性、默认值
        statements.add("ALTER TABLE \"$tableName\" ALTER COLUMN \"$columnName\" TYPE ${mapFieldToColumnType(field)};")

        if (!field.isNullable) {
            statements.add("ALTER TABLE \"$tableName\" ALTER COLUMN \"$columnName\" SET NOT NULL;")
        } else {
            statements.add("ALTER TABLE \"$tableName\" ALTER COLUMN \"$columnName\" DROP NOT NULL;")
        }

        if (field.defaultValue != null) {
            statements.add("ALTER TABLE \"$tableName\" ALTER COLUMN \"$columnName\" SET DEFAULT ${field.defaultValue};")
        }

        return statements.joinToString("\n")
    }

    override fun generateAddForeignKey(tableName: String, foreignKey: ForeignKeyInfo): String {
        return "ALTER TABLE \"$tableName\" ADD CONSTRAINT \"${foreignKey.name}\" FOREIGN KEY (\"${foreignKey.columnName}\") REFERENCES \"${foreignKey.referencedTable}\" (\"${foreignKey.referencedColumn}\");"
    }

    override fun generateAddComment(lsiClass: LsiClass): String {
        val statements = mutableListOf<String>()
        val tableName = lsiClass.guessTableName

        // 表注释
        if (lsiClass.comment != null) {
            statements.add("COMMENT ON TABLE \"$tableName\" IS '${lsiClass.comment}';")
        }

        // 列注释
        lsiClass.getAllDbFields().filter { it.comment != null }.forEach { field ->
            val columnName = field.columnName ?: field.name ?: return@forEach
            statements.add("COMMENT ON COLUMN \"$tableName\".\"$columnName\" IS '${field.comment}';")
        }

        return statements.joinToString("\n")
    }

    override fun generateSchema(lsiClasses: List<LsiClass>): String {
        val statements = mutableListOf<String>()
        
        // 1. 创建所有需要的序列
        val sequences = mutableSetOf<String>()
        lsiClasses.forEach { lsiClass ->
            lsiClass.getAllDbFields().forEach { field ->
                if (field.isSequence) {
                    val seqName = field.sequenceName ?: "${field.columnName ?: field.name}_seq"
                    sequences.add(seqName)
                }
            }
        }
        sequences.forEach { seqName ->
            statements.add(generateCreateSequence(seqName))
        }
        
        // 2. 创建表
        val createTableStatements = lsiClasses.map { lsiClass -> generateCreateTable(lsiClass) }
        statements.addAll(createTableStatements)
        
        // 3. 添加约束和注释
        val addConstraintsStatements = lsiClasses.flatMap { lsiClass ->
            val foreignKeyStatements = lsiClass.getDatabaseForeignKeys().map { fk ->
                generateAddForeignKey(lsiClass.guessTableName, fk)
            }
            val commentStatements = if (lsiClass.comment != null || lsiClass.getAllDbFields().any { it.comment != null }) {
                listOf(generateAddComment(lsiClass))
            } else {
                emptyList()
            }
            foreignKeyStatements + commentStatements
        }
        statements.addAll(addConstraintsStatements)

        return statements.joinToString("\n\n")
    }
    
    /**
     * 生成创建序列的DDL语句
     */
    private fun generateCreateSequence(sequenceName: String): String {
        return "CREATE SEQUENCE IF NOT EXISTS \"$sequenceName\" INCREMENT BY 1 START WITH 1;"
    }

    override fun getColumnTypeName(columnType: DatabaseColumnType, precision: Int?, scale: Int?): String {
        return when (columnType) {
            DatabaseColumnType.INT -> "INTEGER"
            DatabaseColumnType.BIGINT -> "BIGINT"
            DatabaseColumnType.SMALLINT -> "SMALLINT"
            DatabaseColumnType.TINYINT -> "SMALLINT"
            DatabaseColumnType.DECIMAL -> {
                if (precision != null && scale != null) {
                    "DECIMAL($precision, $scale)"
                } else if (precision != null) {
                    "DECIMAL($precision)"
                } else {
                    "DECIMAL"
                }
            }
            DatabaseColumnType.FLOAT -> "REAL"
            DatabaseColumnType.DOUBLE -> "DOUBLE PRECISION"
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
            DatabaseColumnType.LONGTEXT -> "TEXT"
            DatabaseColumnType.DATE -> "DATE"
            DatabaseColumnType.TIME -> "TIME"
            DatabaseColumnType.DATETIME -> "TIMESTAMP"
            DatabaseColumnType.TIMESTAMP -> "TIMESTAMP"
            DatabaseColumnType.BOOLEAN -> "BOOLEAN"
            DatabaseColumnType.BLOB -> "BYTEA"
            DatabaseColumnType.BYTES -> "BYTEA"
        }
    }

    private fun buildColumnDefinition(field: LsiField): String {
        val builder = StringBuilder()
        val columnName = field.columnName ?: field.name ?: "unknown"

        // 使用新的类型映射方法
        val columnTypeName = mapFieldToColumnType(field)

        builder.append("\"$columnName\" $columnTypeName")

        if (!field.isNullable) {
            builder.append(" NOT NULL")
        }

        // PostgreSQL支持三种自增方式：
        // 1. IDENTITY (推荐，PostgreSQL 10+)
        // 2. SERIAL类型 (传统方式)
        // 3. SEQUENCE + DEFAULT nextval() (传统方式)
        if (field.isAutoIncrement) {
            builder.append(" GENERATED BY DEFAULT AS IDENTITY")
        } else if (field.isSequence) {
            // 使用序列：需要配合DEFAULT nextval('seq_name')
            val seqName = field.sequenceName ?: "${columnName}_seq"
            builder.append(" DEFAULT nextval('$seqName')")
        }

        if (field.defaultValue != null && !field.isAutoIncrement && !field.isSequence) {
            builder.append(" DEFAULT ${field.defaultValue}")
        }

        if (field.isPrimaryKey) {
            builder.append(" PRIMARY KEY")
        }

        return builder.toString()
    }

    /**
     * PostgreSQL字符串类型映射
     *
     * PostgreSQL的TEXT类型没有长度限制，性能与VARCHAR相同
     * 推荐策略：
     * - 有明确长度限制的用VARCHAR(n)
     * - 长文本统一用TEXT
     */
    private fun mapStringType(field: LsiField): String {
        // 1. 检查是否为长文本
        if (field.isText) {
            return "TEXT"
        }

        // 2. 普通字符串
        val length = field.length
        return when {
            length > 0 -> "VARCHAR($length)"
            else -> "VARCHAR(255)" // 默认长度
        }
    }
}
