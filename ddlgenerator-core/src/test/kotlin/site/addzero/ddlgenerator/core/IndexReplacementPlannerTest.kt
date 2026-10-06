package site.addzero.ddlgenerator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import site.addzero.ddlgenerator.core.diff.CreateIndex
import site.addzero.ddlgenerator.core.diff.DropIndex
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlIndex
import site.addzero.ddlgenerator.core.model.AutoDdlIndexType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.ddlgenerator.core.options.AutoDdlDiffOptions

class IndexReplacementPlannerTest {
    @Test
    fun `same name with a changed definition requires destructive changes`() {
        val actual = schema(index("uk_shared", "a"))
        val desired = schema(index("uk_shared", "b"))
        assertEquals(emptyList(), SchemaDiffPlanner.plan(desired, actual))
        assertEquals(
            listOf(DropIndex("review_item", "uk_shared"), CreateIndex("review_item", index("uk_shared", "b"))),
            SchemaDiffPlanner.plan(desired, actual, AutoDdlDiffOptions(allowDestructiveChanges = true)),
        )
    }

    @Test
    fun `equivalent definition with an available physical name is retained`() {
        val actual = schema(index("uk_old", "a"))
        val desired = schema(index("uk_new", "a"))
        for (destructive in listOf(false, true)) {
            assertEquals(emptyList(), SchemaDiffPlanner.plan(desired, actual,
                AutoDdlDiffOptions(allowDestructiveChanges = destructive)))
        }
    }

    @Test
    fun `retained equivalent index cannot occupy another desired index name`() {
        val actual = schema(index("uk_shared", "a"))
        val desiredIndexes = listOf(index("uk_shared", "b"), index("uk_a", "a"))
        val expected = listOf(
            DropIndex("review_item", "uk_shared"),
            CreateIndex("review_item", index("uk_a", "a")),
            CreateIndex("review_item", index("uk_shared", "b")),
        )
        for (indexes in listOf(desiredIndexes, desiredIndexes.reversed())) {
            assertEquals(expected, SchemaDiffPlanner.plan(schema(*indexes.toTypedArray()), actual,
                AutoDdlDiffOptions(allowDestructiveChanges = true)))
        }
    }

    @Test
    fun `collision stays pending without destructive changes`() {
        val actual = schema(index("uk_shared", "a"))
        val desired = schema(index("uk_shared", "b"), index("uk_a", "a"))
        assertEquals(emptyList(), SchemaDiffPlanner.plan(desired, actual))
    }

    @Test
    fun `swapped definitions release both physical names before creation`() {
        val actual = schema(index("uk_a", "a"), index("uk_b", "b"))
        val desired = schema(index("uk_a", "b"), index("uk_b", "a"))
        assertEquals(
            listOf(
                DropIndex("review_item", "uk_a"),
                DropIndex("review_item", "uk_b"),
                CreateIndex("review_item", index("uk_a", "b")),
                CreateIndex("review_item", index("uk_b", "a")),
            ),
            SchemaDiffPlanner.plan(desired, actual, AutoDdlDiffOptions(allowDestructiveChanges = true)),
        )
    }

    @Test
    fun `physical name conflicts are case insensitive`() {
        val actual = schema(index("UK_SHARED", "a"))
        val desired = schema(index("uk_shared", "b"), index("uk_a", "a"))
        assertEquals(
            listOf(
                DropIndex("review_item", "UK_SHARED"),
                CreateIndex("review_item", index("uk_a", "a")),
                CreateIndex("review_item", index("uk_shared", "b")),
            ),
            SchemaDiffPlanner.plan(desired, actual, AutoDdlDiffOptions(allowDestructiveChanges = true)),
        )
    }

    private fun index(name: String, column: String) = AutoDdlIndex(name, listOf(column), AutoDdlIndexType.UNIQUE)

    private fun schema(vararg indexes: AutoDdlIndex) = AutoDdlSchema(
        listOf(AutoDdlTable("review_item", emptyList(), indexes = indexes.toList()))
    )
}
