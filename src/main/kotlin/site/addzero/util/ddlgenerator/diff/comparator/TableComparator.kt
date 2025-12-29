package site.addzero.util.ddlgenerator.diff.comparator

import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.lsi.clazz.LsiClass

/**
 * 表比对器接口
 */
interface TableComparator {
    fun compare(lsiClass: List<LsiClass>, dbTable: List<JdbcTableMetadata>?):List<String>
}

