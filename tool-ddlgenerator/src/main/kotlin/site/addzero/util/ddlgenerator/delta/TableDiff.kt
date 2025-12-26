package site.addzero.util.ddlgenerator.delta

import site.addzero.entity.JdbcColumnMetadata
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.field.LsiField

/**
 * 表差异的基类
 */
sealed class TableDiff {
    /**
     * 新增表
     */
    data class NewTable(val lsiClass: LsiClass) : TableDiff()

    /**
     * 删除表
     */
    data class DroppedTable(val tableName: String, val schema: String? = null) : TableDiff()

    /**
     * 修改表
     */
    data class ModifiedTable(
        val tableName: String,
        val schema: String? = null,
        val addedColumns: List<LsiField> = emptyList(),
        val droppedColumns: List<JdbcColumnMetadata> = emptyList(),
        val modifiedColumns: List<ColumnModification> = emptyList()
    ) : TableDiff() {
        val hasChanges: Boolean
            get() = addedColumns.isNotEmpty() || droppedColumns.isNotEmpty() || modifiedColumns.isNotEmpty()
    }

    /**
     * 无变化
     */
    data object NoChange : TableDiff()
}

/**
 * 列修改信息
 */
data class ColumnModification(
    val field: LsiField,
    val oldColumn: JdbcColumnMetadata,
    val changes: Set<ColumnChangeType>
) {
    enum class ColumnChangeType {
        TYPE_CHANGED,       // 类型变化
        LENGTH_CHANGED,     // 长度变化
        NULLABLE_CHANGED,   // 可空性变化
        DEFAULT_CHANGED,    // 默认值变化
        COMMENT_CHANGED     // 注释变化
    }
}

/**
 * Schema 差异
 */
