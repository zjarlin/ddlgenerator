package site.addzero.util.ddlgenerator.diff.comparator

import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.ddlgenerator.inter.DdlSettingContext
import site.addzero.util.lsi.clazz.LsiClass

/**
 * 表比对器接口
 */
interface TableComparator {
    fun compare(lsiClass: LsiClass, dbTable: JdbcTableMetadata?, config: DdlSettingContext)
}

