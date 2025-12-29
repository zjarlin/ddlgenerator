package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.Types

/**
 * H2 类型映射器
 */
object H2TypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
        Types.BIT -> "BOOLEAN"
        Types.TINYINT -> "TINYINT"
        Types.SMALLINT -> "SMALLINT"
        Types.INTEGER -> "INT"
        Types.BIGINT -> "BIGINT"
        Types.FLOAT -> "REAL"
        Types.REAL -> "REAL"
        Types.DOUBLE -> "DOUBLE"
        Types.NUMERIC -> "NUMERIC"
        Types.DECIMAL -> "DECIMAL"
        Types.CHAR -> "CHAR"
        Types.VARCHAR -> "VARCHAR"
        Types.LONGVARCHAR -> "CLOB"
        Types.NCHAR -> "CHAR"
        Types.NVARCHAR -> "VARCHAR"
        Types.LONGNVARCHAR -> "CLOB"
        Types.CLOB -> "CLOB"
        Types.NCLOB -> "CLOB"
        Types.DATE -> "DATE"
        Types.TIME -> "TIME"
        Types.TIMESTAMP -> "TIMESTAMP"
        Types.TIME_WITH_TIMEZONE -> "TIME"
        Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP"
        Types.BINARY -> "BINARY"
        Types.VARBINARY -> "VARBINARY"
        Types.LONGVARBINARY -> "BLOB"
        Types.BLOB -> "BLOB"
        Types.BOOLEAN -> "BOOLEAN"
        Types.ARRAY -> "ARRAY"
        Types.SQLXML -> "JSON"
        else -> "OTHER"
    }

    override fun toJdbcType(nativeTypeName: String): Int? {
        val upper = nativeTypeName.trim().uppercase()
        return when {
            upper.startsWith("TINYINT") -> Types.TINYINT
            upper.startsWith("SMALLINT") -> Types.SMALLINT
            upper.startsWith("INT") || upper.startsWith("INTEGER") -> Types.INTEGER
            upper.startsWith("BIGINT") -> Types.BIGINT
            upper.startsWith("DECIMAL") || upper.startsWith("NUMERIC") -> Types.DECIMAL
            upper.startsWith("FLOAT") || upper == "REAL" -> Types.REAL
            upper.startsWith("DOUBLE") -> Types.DOUBLE
            upper.startsWith("VARCHAR(") || upper == "VARCHAR" -> Types.VARCHAR
            upper.startsWith("CHAR(") || upper == "CHAR" -> Types.CHAR
            upper in listOf("CLOB", "TEXT") -> Types.CLOB
            upper == "DATE" -> Types.DATE
            upper == "TIME" || upper.startsWith("TIME(") -> Types.TIME
            upper == "TIMESTAMP" || upper.startsWith("TIMESTAMP(") -> Types.TIMESTAMP
            upper in listOf("BOOLEAN", "BIT") -> Types.BOOLEAN
            upper in listOf("BLOB", "BINARY") -> Types.BLOB
            upper.startsWith("VARBINARY(") || upper == "VARBINARY" -> Types.VARBINARY
            upper == "UUID" -> Types.OTHER
            upper == "JSON" -> Types.OTHER
            upper.startsWith("ARRAY") -> Types.ARRAY
            else -> null
        }
    }
}
