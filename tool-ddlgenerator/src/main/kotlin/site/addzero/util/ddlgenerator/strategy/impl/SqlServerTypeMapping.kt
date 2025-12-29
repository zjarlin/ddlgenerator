package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.Types

/**
 * SQL Server 类型映射器
 */
object SqlServerTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
        Types.BIT -> "BIT"
        Types.TINYINT -> "TINYINT"
        Types.SMALLINT -> "SMALLINT"
        Types.INTEGER -> "INT"
        Types.BIGINT -> "BIGINT"
        Types.FLOAT -> "FLOAT"
        Types.REAL -> "REAL"
        Types.DOUBLE -> "FLOAT"
        Types.NUMERIC -> "NUMERIC"
        Types.DECIMAL -> "DECIMAL"
        Types.CHAR -> "CHAR"
        Types.VARCHAR -> "VARCHAR"
        Types.LONGVARCHAR -> "VARCHAR(MAX)"
        Types.NCHAR -> "NCHAR"
        Types.NVARCHAR -> "NVARCHAR"
        Types.LONGNVARCHAR -> "NVARCHAR(MAX)"
        Types.CLOB -> "VARCHAR(MAX)"
        Types.NCLOB -> "NVARCHAR(MAX)"
        Types.DATE -> "DATE"
        Types.TIME -> "TIME"
        Types.TIMESTAMP -> "DATETIME2"
        Types.TIME_WITH_TIMEZONE -> "DATETIMEOFFSET"
        Types.TIMESTAMP_WITH_TIMEZONE -> "DATETIMEOFFSET"
        Types.BINARY -> "BINARY"
        Types.VARBINARY -> "VARBINARY"
        Types.LONGVARBINARY -> "VARBINARY(MAX)"
        Types.BLOB -> "VARBINARY(MAX)"
        Types.BOOLEAN -> "BIT"
        Types.SQLXML -> "XML"
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
            upper == "REAL" -> Types.REAL
            upper.startsWith("VARCHAR(") || upper == "VARCHAR" -> Types.VARCHAR
            upper.startsWith("NVARCHAR(") || upper == "NVARCHAR" -> Types.NVARCHAR
            upper.startsWith("CHAR(") || upper == "CHAR" -> Types.CHAR
            upper.startsWith("NCHAR(") || upper == "NCHAR" -> Types.NCHAR
            upper == "TEXT" || upper == "NTEXT" || upper == "VARCHAR(MAX)" || upper == "NVARCHAR(MAX)" -> Types.LONGVARCHAR
            upper == "DATE" -> Types.DATE
            upper == "TIME" || upper.startsWith("TIME(") -> Types.TIME
            upper in listOf("DATETIME", "DATETIME2", "DATETIMEOFFSET", "SMALLDATETIME") -> Types.TIMESTAMP
            upper == "BIT" -> Types.BIT
            upper == "BINARY" || upper.startsWith("BINARY(") -> Types.BINARY
            upper == "VARBINARY" || upper.startsWith("VARBINARY(") || upper == "VARBINARY(MAX)" || upper == "IMAGE" -> Types.VARBINARY
            upper == "UNIQUEIDENTIFIER" -> Types.OTHER
            upper == "XML" -> Types.SQLXML
            else -> null
        }
    }
}
