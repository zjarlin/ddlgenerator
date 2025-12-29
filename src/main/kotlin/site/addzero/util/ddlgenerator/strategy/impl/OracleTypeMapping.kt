package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.Types

/**
 * Oracle 类型映射器
 */
object OracleTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
        Types.BIT -> "NUMBER(1)"
        Types.TINYINT -> "NUMBER(3)"
        Types.SMALLINT -> "NUMBER(5)"
        Types.INTEGER -> "NUMBER(10)"
        Types.BIGINT -> "NUMBER(19)"
        Types.FLOAT -> "BINARY_FLOAT"
        Types.REAL -> "BINARY_FLOAT"
        Types.DOUBLE -> "BINARY_DOUBLE"
        Types.NUMERIC -> "NUMBER"
        Types.DECIMAL -> "NUMBER"
        Types.CHAR -> "CHAR"
        Types.VARCHAR -> "VARCHAR2"
        Types.LONGVARCHAR -> "CLOB"
        Types.NCHAR -> "NCHAR"
        Types.NVARCHAR -> "NVARCHAR2"
        Types.LONGNVARCHAR -> "NCLOB"
        Types.CLOB -> "CLOB"
        Types.NCLOB -> "NCLOB"
        Types.DATE -> "DATE"
        Types.TIME -> "TIMESTAMP"
        Types.TIMESTAMP -> "TIMESTAMP"
        Types.TIME_WITH_TIMEZONE -> "TIMESTAMP WITH TIME ZONE"
        Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP WITH TIME ZONE"
        Types.BINARY -> "RAW"
        Types.VARBINARY -> "RAW"
        Types.LONGVARBINARY -> "BLOB"
        Types.BLOB -> "BLOB"
        Types.BOOLEAN -> "NUMBER(1)"
        Types.SQLXML -> "JSON"
        else -> "OTHER"
    }

    override fun toJdbcType(nativeTypeName: String): Int? {
        val upper = nativeTypeName.trim().uppercase()
        return when {
            upper.startsWith("NUMBER(") -> when {
                upper.contains("(3)") -> Types.TINYINT
                upper.contains("(5)") -> Types.SMALLINT
                upper.contains("(10)") -> Types.INTEGER
                upper.contains("(19)") -> Types.BIGINT
                upper.contains("(1)") -> Types.BOOLEAN
                else -> Types.DECIMAL
            }
            upper == "NUMBER" -> Types.DECIMAL
            upper == "BINARY_FLOAT" -> Types.FLOAT
            upper == "BINARY_DOUBLE" -> Types.DOUBLE
            upper.startsWith("VARCHAR2(") || upper == "VARCHAR2" -> Types.VARCHAR
            upper.startsWith("VARCHAR(") || upper == "VARCHAR" -> Types.VARCHAR
            upper.startsWith("CHAR(") || upper == "CHAR" -> Types.CHAR
            upper == "CLOB" -> Types.CLOB
            upper == "NCLOB" -> Types.NCLOB
            upper == "DATE" -> Types.DATE
            upper.startsWith("TIMESTAMP") -> Types.TIMESTAMP
            upper.startsWith("INTERVAL") -> Types.TIME
            upper == "BLOB" -> Types.BLOB
            upper.startsWith("RAW(") || upper == "RAW" -> Types.BINARY
            upper == "JSON" -> Types.OTHER
            else -> null
        }
    }
}
