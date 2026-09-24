package site.addzero.ddlgenerator.core

import site.addzero.ddlgenerator.core.diff.CreateIndex
import site.addzero.ddlgenerator.core.diff.DropIndex
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlIndex
import site.addzero.ddlgenerator.core.model.AutoDdlIndexType
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.ddlgenerator.core.options.AutoDdlDiffOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexReplacementTest {
    private val corrected = AutoDdlIndex("uk_member_key", listOf("parent_id", "name"), AutoDdlIndexType.UNIQUE)
    private val legacy = corrected.copy(columnNames = listOf("name"))

    @Test
    fun `same name cannot hide different unique columns or index type`() {
        for (old in listOf(legacy, corrected.copy(type = AutoDdlIndexType.NORMAL))) {
            val changes = SchemaDiffPlanner.plan(schema(corrected), schema(old), AutoDdlDiffOptions(allowDestructiveChanges = true))
            assertEquals(listOf(DropIndex("member", old.name), CreateIndex("member", corrected)), changes)
        }
    }

    @Test
    fun `same name replacement waits for destructive changes instead of attempting a no op create`() {
        assertTrue(SchemaDiffPlanner.plan(schema(corrected), schema(legacy)).isEmpty())
    }

    @Test
    fun `equivalent definition under another name needs no migration`() {
        val changes = SchemaDiffPlanner.plan(schema(corrected), schema(corrected.copy(name = "existing_constraint")), AutoDdlDiffOptions(allowDestructiveChanges = true))
        assertTrue(changes.isEmpty())
    }

    private fun schema(index: AutoDdlIndex) = AutoDdlSchema(listOf(AutoDdlTable(
        name = "member",
        columns = listOf(
            AutoDdlColumn("parent_id", AutoDdlLogicalType.INT64, nullable = false),
            AutoDdlColumn("name", AutoDdlLogicalType.STRING, nullable = false),
        ),
        indexes = listOf(index),
    )))
}
