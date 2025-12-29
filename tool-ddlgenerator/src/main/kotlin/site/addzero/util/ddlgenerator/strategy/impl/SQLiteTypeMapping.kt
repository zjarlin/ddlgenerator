package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.Types

/**
 * SQLite 类型映射器
 *
 * SQLite 使用类型亲和性，类型比较宽松
 */
object SQLiteTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
        Types.BIT, Types.TINYINT, Types.SMALLINT, Types.INTEGER -> "INTEGER"
        Types.BIGINT -> "INTEGER"
        Types.FLOAT, Types.REAL, Types.DOUBLE -> "REAL"
        Types.NUMERIC, Types.DECIMAL -> "NUMERIC"
        Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> "TEXT"
        Types.CLOB, Types.NCLOB -> "TEXT"
        Types.DATE, Types.TIME, Types.TIMESTAMP, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE -> "INTEGER"
        Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "BLOB"
        Types.BOOLEAN -> "INTEGER"
        Types.ARRAY -> "TEXT"
        else -> "TEXT"
    }

    override fun toJdbcType(nativeTypeName: String): Int? {
        val upper = nativeTypeName.trim().uppercase()
        return when {
            // SQLite 的类型亲和性
            upper in listOf("TINYINT", "SMALLINT", "INT", "INTEGER", "BIGINT",
                           "MEDIUMINT", "INT2", "INT8") -> Types.INTEGER
            upper in listOf("DECIMAL", "NUMERIC") -> Types.DECIMAL
            upper in listOf("FLOAT", "DOUBLE", "REAL") -> Types.REAL
            upper in listOf("CHAR", "VARCHAR", "TEXT", "CLOB", "VARYING",
                           "NCHAR", "NVARCHAR", "NVCHAR") -> Types.LONGVARCHAR
            upper in listOf("BLOB", "BINARY") -> Types.BLOB
            else -> null
        }
    }
}
