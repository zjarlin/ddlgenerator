package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.JDBCType
import java.sql.Types

/**
 * MySQL 类型映射器
 */
object MySqlTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {

        Types.BIT ->JDBCType.BIT.name
        Types.TINYINT -> "TINYINT"
        Types.SMALLINT -> "SMALLINT"
        Types.INTEGER -> "INT"
        Types.BIGINT -> "BIGINT"
        Types.FLOAT -> "FLOAT"
        Types.REAL -> "FLOAT"
        Types.DOUBLE -> "DOUBLE"
        Types.NUMERIC -> "NUMERIC"
        Types.DECIMAL -> "DECIMAL"
        Types.CHAR -> "CHAR"
        Types.VARCHAR -> "VARCHAR"
        Types.LONGVARCHAR -> "LONGTEXT"
        Types.NCHAR -> "NCHAR"
        Types.NVARCHAR -> "NVARCHAR"
        Types.LONGNVARCHAR -> "LONGTEXT"
        Types.CLOB -> "LONGTEXT"
        Types.NCLOB -> "LONGTEXT"
        Types.DATE -> "DATE"
        Types.TIME -> "TIME"
        Types.TIMESTAMP -> "TIMESTAMP"
        Types.TIME_WITH_TIMEZONE -> "TIME"
        Types.TIMESTAMP_WITH_TIMEZONE -> "TIMESTAMP"
        Types.BINARY -> "BINARY"
        Types.VARBINARY -> "VARBINARY"
        Types.LONGVARBINARY -> "LONGBLOB"
        Types.BLOB -> "BLOB"
        Types.BOOLEAN -> "TINYINT(1)"
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
            upper.startsWith("FLOAT") -> Types.FLOAT
            upper.startsWith("DOUBLE") -> Types.DOUBLE
            upper.startsWith("VARCHAR(") || upper == "VARCHAR" -> Types.VARCHAR
            upper.startsWith("CHAR(") || upper == "CHAR" -> Types.CHAR
            upper == "TEXT" || upper == "LONGTEXT" || upper.startsWith("TINYTEXT") || upper.startsWith("MEDIUMTEXT") -> Types.LONGVARCHAR
            upper == "DATE" -> Types.DATE
            upper == "TIME" || upper.startsWith("TIME(") -> Types.TIME
            upper == "DATETIME" || upper.startsWith("DATETIME(") || upper == "TIMESTAMP" || upper.startsWith("TIMESTAMP(") -> Types.TIMESTAMP
            upper in listOf("TINYINT(1)", "BOOLEAN", "BOOL") -> Types.BOOLEAN
            upper == "BIT" || upper.startsWith("BIT(") -> Types.BIT
            upper == "BLOB" || upper.startsWith("TINYBLOB") || upper.startsWith("MEDIUMBLOB") || upper == "LONGBLOB" -> Types.BLOB
            upper.startsWith("BINARY(") || upper == "BINARY" -> Types.BINARY
            upper.startsWith("VARBINARY(") || upper == "VARBINARY" -> Types.VARBINARY
            upper == "JSON" -> Types.SQLXML
            else -> null
        }
    }
}
