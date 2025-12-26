package site.addzero.util.ddlgenerator.delta

import site.addzero.util.ddlgenerator.inter.DdlSettingContext


/**
 * 差量 DDL 生成器
 * 根据差异模型生成增量 SQL 语句
 */
class DeltaDdlGenerator(
    private val settings: DdlSettingContext,
) {

    private val strategy by lazy {
        settings.strategy
    }

    /**
     * 生成 Schema 差量 DDL
     */

    /**
     * 生成单个表的差量 DDL
     */
    fun generateTableDeltaDdl(diff: TableDiff): String {
        return when (diff) {
            is TableDiff.NewTable -> {
                buildString {
                    appendLine("-- Create new table: ${diff.lsiClass.name}")
                    append(strategy.generateCreateTable(diff.lsiClass))
                }
            }

            is TableDiff.DroppedTable -> {
                buildString {
                    appendLine("-- Drop table: ${diff.tableName}")
                    append(strategy.generateDropTable(diff.tableName))
                }
            }

            is TableDiff.ModifiedTable -> {
                buildString {
                    appendLine("-- Modify table: ${diff.tableName}")

                    // 新增列
                    diff.addedColumns.forEach { field ->
                        appendLine(strategy.generateAddColumn(diff.tableName, field))
                    }

                    // 修改列
                    diff.modifiedColumns.forEach { modification ->
                        appendLine(
                            "-- Modify column: ${modification.field.name} " +
                                    "(changes: ${modification.changes.joinToString(", ")})"
                        )
                        appendLine(strategy.generateModifyColumn(diff.tableName, modification.field))
                    }

                    // 删除列
                    diff.droppedColumns.forEach { column ->
                        appendLine(strategy.generateDropColumn(diff.tableName, column.columnName))
                    }
                }
            }

            is TableDiff.NoChange -> "-- No changes"
        }
    }
}
