package site.addzero.util.ddlgenerator.assist

import site.addzero.util.lsi.field.LsiField
import site.addzero.util.lsi_impl.impl.database.field.isText
import site.addzero.util.lsi_impl.impl.database.field.length

/**
 * String分为长文本
 * @param [field]
 * @return [String]
 */
internal fun mapStringType(field: LsiField): String {
    // 1. 检查是否为长文本
    if (field.isText) {
        val length = field.length
        return when {
            length > 16_777_215 -> "LONGTEXT"
            length > 65_535 -> "MEDIUMTEXT"
            else -> "TEXT"
        }
    }

    // 2. 普通字符串
    val length = field.length
    return when {
        length > 0 -> "VARCHAR($length)"
        else -> "VARCHAR(255)" // 默认长度
    }
}


/**
 * 默认映射
 */
internal val defaultSimpleTypeMappings: MutableMap<String, (LsiField) -> String>
    get() = mutableMapOf(
        "String" to { field ->
            mapStringType(field)
        },
        "Long" to { "BIGINT" },
        "Short" to { "SMALLINT" },
        "Short" to { "BIGINT" },
    )
