package site.addzero.util.ddlgenerator.strategy.impl

import site.addzero.util.lsi.database.dialect.DatabaseTypeMapping
import java.sql.JDBCType
import java.sql.Types
import kotlin.enums.enumEntries


/**
 * DM (达梦/Dameng) 类型映射器
 */
class DmTypeMapping : DatabaseTypeMapping {

    override fun getByJdbcType(jdbcType: Int): String = when (jdbcType) {
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
            upper == "REAL" -> Types.REAL
            upper.startsWith("DOUBLE") -> Types.DOUBLE
            upper.startsWith("VARCHAR(") || upper == "VARCHAR" -> Types.VARCHAR
            upper.startsWith("CHAR(") || upper == "CHAR" -> Types.CHAR
            upper in listOf("TEXT", "CLOB") -> Types.CLOB
            upper == "DATE" -> Types.DATE
            upper == "TIME" || upper.startsWith("TIME(") -> Types.TIME
            upper.startsWith("TIMESTAMP") -> Types.TIMESTAMP
            upper == "BIT" -> Types.BIT
            upper in listOf("BLOB", "BINARY") -> Types.BLOB
            upper.startsWith("VARBINARY(") || upper == "VARBINARY" -> Types.VARBINARY
            else -> null
        }
    }

    override fun jdbctype2dialectType(jdbcType: JDBCType): String {

        val enumEntries = enumEntries<JDBCType>()
        val diffMap = mapOf<JDBCType, String>(
            JDBCType.INTEGER to "INT",
            JDBCType.LONGVARCHAR to "CLOB",
            JDBCType.LONGNVARCHAR to "CLOB",
            JDBCType.NCLOB to "CLOB",
            JDBCType.TIME_WITH_TIMEZONE to "TIME",
            JDBCType.TIMESTAMP_WITH_TIMEZONE to "TIMESTAMP",
            JDBCType.LONGVARBINARY to "BLOB",
            JDBCType.BOOLEAN to "BIT",
            JDBCType.SQLXML to "CLOB",
        )

        val map = enumEntries.associate {
            val name = it.name
            it to name
        }
        val map1 = map + diffMap
        val string = map1[jdbcType]
        return string?:"OTHER"
    }
}
