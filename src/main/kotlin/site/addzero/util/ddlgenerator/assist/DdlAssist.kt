package site.addzero.util.ddlgenerator.assist

import site.addzero.util.lsi.field.LsiField
import site.addzero.util.lsi_impl.impl.database.field.isText
import site.addzero.util.lsi_impl.impl.database.field.length
import java.sql.Types

/**
 * 将字段转换为 JDBC 类型代码
 *
 * @param databaseType 数据库类型
 * @return JDBC 类型代码 (java.sql.Types.*)
 */
internal fun LsiField.toJdbcTypeCode(): Int {
    return when (typeName) {
        "String" -> {
            // 字符串类型：根据长度决定
            if (isText) {
                // 长文本
                when {
                    length > 16_777_215 -> Types.LONGVARCHAR  // MEDIUMBLOB/LONGTEXT
                    length > 65_535 -> Types.CLOB           // 64KB+
                    else -> Types.LONGVARCHAR               // TEXT
                }
            } else {
                // 普通 VARCHAR
                Types.VARCHAR
            }
        }

        "Long", "java.lang.Long" -> Types.BIGINT
        "Integer", "java.lang.Integer" -> Types.INTEGER
        "Short", "java.lang.Short" -> Types.SMALLINT
        "Byte", "java.lang.Byte" -> Types.TINYINT
        "Float", "java.lang.Float" -> Types.REAL
        "Double", "java.lang.Double" -> Types.DOUBLE
        "BigDecimal", "java.math.BigDecimal" -> Types.DECIMAL
        "Boolean", "java.lang.Boolean" -> Types.BOOLEAN
        "java.util.Date", "java.sql.Date", "LocalDate" -> Types.DATE
        "java.sql.Time", "LocalTime" -> Types.TIME
        "java.sql.Timestamp", "LocalDateTime" -> Types.TIMESTAMP
        "byte[]" -> Types.BLOB
        "ByteBuffer", "java.nio.ByteBuffer" -> Types.BLOB
        else -> Types.VARCHAR  // 默认
    }
}

