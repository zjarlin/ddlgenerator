package site.addzero.ddlgenerator.core

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import site.addzero.ddlgenerator.core.dialect.AbstractSqlDialect
import site.addzero.ddlgenerator.core.diff.CreateTable
import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.util.db.DatabaseType

class AutoDdlDialectTest {
    private val dialect = object : AbstractSqlDialect(DatabaseType.H2) {
        override fun supportsInlinePrimaryKey(column: AutoDdlColumn): Boolean = column.primaryKey
    }

    @Test
    fun `composite primary key is rendered once at table level`() {
        val table = AutoDdlTable("review_item", listOf(
            AutoDdlColumn("tenant_id", AutoDdlLogicalType.INT64, nullable = false, primaryKey = true),
            AutoDdlColumn("item_id", AutoDdlLogicalType.INT64, nullable = false, primaryKey = true),
        ))
        val sql = dialect.render(CreateTable(table)).single()
        assertContains(sql, "PRIMARY KEY (\"tenant_id\", \"item_id\")")
        assertEquals(1, Regex("PRIMARY KEY").findAll(sql).count(), sql)
    }

    @Test
    fun `single column primary key remains inline`() {
        val table = AutoDdlTable("review_item", listOf(
            AutoDdlColumn("item_id", AutoDdlLogicalType.INT64, nullable = false, primaryKey = true),
        ))
        val sql = dialect.render(CreateTable(table)).single()
        assertContains(sql, "\"item_id\" BIGINT NOT NULL PRIMARY KEY")
        assertEquals(1, Regex("PRIMARY KEY").findAll(sql).count(), sql)
    }
}
