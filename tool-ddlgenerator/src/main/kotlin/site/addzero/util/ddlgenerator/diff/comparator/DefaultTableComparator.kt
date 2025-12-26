package site.addzero.util.ddlgenerator.diff.comparator

import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.ddlgenerator.delta.ColumnModification
import site.addzero.util.ddlgenerator.delta.TableDiff
import site.addzero.util.ddlgenerator.diff.matcher.ColumnMatcher
import site.addzero.util.ddlgenerator.inter.DdlSettingContext
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi_impl.impl.database.clazz.getAllDbFields

/**
 * 默认表比对器实现
 */
class DefaultTableComparator : TableComparator {

    override fun compare(lsiClass: LsiClass, dbTable: JdbcTableMetadata?, config: Settings): TableDiff {
        // 如果数据库表不存在，返回新增表
        if (dbTable == null) {
            return TableDiff.NewTable(lsiClass)
        }

        val tableName = lsiClass.guessTableName
        val lsiFields = lsiClass.getAllDbFields()
        val dbColumns = dbTable.columns

        // 按名称建立映射（忽略大小写）
        val dbColumnMap = dbColumns.associateBy {
            if (config.ignoreCase) it.columnName.lowercase() else it.columnName
        }
        val lsiFieldMap = lsiFields.associateBy { field ->
            val name = field.columnName ?: field.name ?: ""
            if (config.ignoreCase) name.lowercase() else name
        }

        // 找出新增的列
        val addedColumns = lsiFields.filter { field ->
            val name = field.columnName ?: field.name ?: ""
            val key = if (config.ignoreCase) name.lowercase() else name
            !dbColumnMap.containsKey(key)
        }

        // 找出删除的列
        val droppedColumns = if (config.allowDrop) {
            dbColumns.filter { column ->
                val key = if (config.ignoreCase) column.columnName.lowercase() else column.columnName
                !lsiFieldMap.containsKey(key)
            }
        } else {
            emptyList()
        }

        // 找出修改的列
        val modifiedColumns = lsiFields.mapNotNull { field ->
            val name = field.columnName ?: field.name ?: ""
            val key = if (config.ignoreCase) name.lowercase() else name
            val dbColumn = dbColumnMap[key] ?: return@mapNotNull null

            val changes = ColumnMatcher.detectChanges(field, dbColumn, config)
            if (changes.isNotEmpty()) {
                ColumnModification(field, dbColumn, changes)
            } else {
                null
            }
        }

        // 如果有任何变化，返回修改表
        if (addedColumns.isNotEmpty() || droppedColumns.isNotEmpty() || modifiedColumns.isNotEmpty()) {
            return TableDiff.ModifiedTable(
                tableName = tableName,
                schema = dbTable.schema,
                addedColumns = addedColumns,
                droppedColumns = droppedColumns,
                modifiedColumns = modifiedColumns
            )
        }

        return TableDiff.NoChange
    }

}