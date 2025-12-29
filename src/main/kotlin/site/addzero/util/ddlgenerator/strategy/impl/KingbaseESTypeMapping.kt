package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.Types

/**
 * KingbaseES (人大金仓) 类型映射器
 */
object KingbaseESTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
        Types.BIT -> "BIT"
        Types.TINYINT -> "SMALLINT"
        Types.SMALLINT -> "SMALLINT"
        Types.INTEGER -> "INTEGER"
        Types.BIGINT -> "BIGINT"
        Types.FLOAT -> "REAL"
        Types.REAL -> "REAL"
        Types.DOUBLE -> "DOUBLE PRECISION"
        Types.NUMERIC -> "NUMERIC"
        Types.DECIMAL -> "NUMERIC"
        Types.CHAR -> "CHAR"
        Types.VARCHAR -> "VARCHAR"
        Types.LONGVARCHAR -> "TEXT"
        Types.NCHAR -> "CHAR"
        Types.NVARCHAR -> "VARCHAR"
        Types.LONGNVARCHAR -> "TEXT"
        Types.CLOB -> "TEXT"
        Types.NCLOB -> "TEXT"
        Types.DATE -> "DATE"
        Types.TIME -> "TIME"
        Types.TIMESTAMP -> "TIMESTAMP"
        Types.TIME_WITH_TIMEZONE -> "TIME WITH TIME ZONE"
        Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP WITH TIME ZONE"
        Types.BINARY -> "BYTEA"
        Types.VARBINARY -> "BYTEA"
        Types.LONGVARBINARY -> "BYTEA"
        Types.BLOB -> "BYTEA"
        Types.BOOLEAN -> "BOOLEAN"
        Types.ARRAY -> "ARRAY"
        Types.SQLXML -> "JSONB"
        else -> "OTHER"
    }

    override fun toJdbcType(nativeTypeName: String): Int? {
        val upper = nativeTypeName.trim().uppercase()
        return when {
            upper.startsWith("INTEGER") || upper.startsWith("INT") || upper.startsWith("SERIAL") -> Types.INTEGER
            upper.startsWith("SMALLINT") || upper.startsWith("SMALLSERIAL") -> Types.SMALLINT
            upper.startsWith("BIGINT") || upper.startsWith("BIGSERIAL") -> Types.BIGINT
            upper in listOf("NUMERIC", "DECIMAL") -> Types.DECIMAL
            upper == "REAL" || upper == "FLOAT4" -> Types.REAL
            upper in listOf("DOUBLE PRECISION", "FLOAT8") -> Types.DOUBLE
            upper.startsWith("VARCHAR") || upper.startsWith("CHARACTER VARYING") -> Types.VARCHAR
            upper.startsWith("CHAR(") || upper.startsWith("CHARACTER(") -> Types.CHAR
            upper == "CHAR" || upper == "BPCHAR" -> Types.CHAR
            upper == "TEXT" -> Types.LONGVARCHAR
            upper == "DATE" -> Types.DATE
            upper == "TIME" || upper.startsWith("TIME ") || upper.startsWith("TIME(") -> Types.TIME
            upper.startsWith("TIME WITH TIME ZONE") || upper.startsWith("TIMETZ") -> Types.TIME_WITH_TIMEZONE
            upper.startsWith("TIMESTAMP") && !upper.contains("WITH TIME ZONE") && !upper.startsWith("TIMESTAMPTZ") -> Types.TIMESTAMP
            upper.startsWith("TIMESTAMP WITH TIME ZONE") || upper.startsWith("TIMESTAMPTZ") -> Types.TIMESTAMP_WITH_TIMEZONE
            upper in listOf("BOOLEAN", "BOOL") -> Types.BOOLEAN
            upper in listOf("BYTEA", "BLOB") -> Types.BLOB
            upper.startsWith("ARRAY") || upper.endsWith("[]") -> Types.ARRAY
            upper == "JSON" || upper == "JSONB" -> Types.OTHER
            upper == "UUID" -> Types.OTHER
            else -> null
        }
    }
}
