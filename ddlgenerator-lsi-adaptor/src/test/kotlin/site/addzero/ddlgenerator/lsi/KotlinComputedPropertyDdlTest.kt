package site.addzero.ddlgenerator.lsi

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import java.io.ByteArrayOutputStream
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import site.addzero.ddlgenerator.core.diff.AddColumn
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.dialect.h2.H2AutoDdlDialect
import site.addzero.ddlgenerator.dialect.mysql.MySqlAutoDdlDialect
import site.addzero.ddlgenerator.dialect.postgresql.PostgreSqlAutoDdlDialect
import site.addzero.lsi.ksp.clazz.KspLsiClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCompilerApi::class)
class KotlinComputedPropertyDdlTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `create ddl excludes simple complex inherited and embedded getters`() {
        val schema = compile("create", computed = true)
        assertEquals(setOf("id", "first_name", "last_name", "street"), columns(schema))
        for (dialect in dialects()) {
            val normalized = dialect.normalizeSchema(schema)
            val sql = dialect.render(SchemaDiffPlanner.plan(normalized, AutoDdlSchema(tables = emptyList()))).joinToString("\n")
            assertTrue(sql.contains("CREATE TABLE"), sql)
            assertComputedColumnsAbsent(sql)
        }
    }

    @Test
    fun `adding only computed getters produces no migration in every dialect`() {
        val before = compile("before", computed = false)
        val after = compile("after", computed = true)
        for (dialect in dialects()) {
            val operations = SchemaDiffPlanner.plan(dialect.normalizeSchema(after), dialect.normalizeSchema(before))
            assertTrue(operations.isEmpty(), operations.toString())
        }
    }

    @Test
    fun `incremental ddl adds only stored fields and becomes idempotent`() {
        val before = compile("before", computed = false)
        val after = compile("after", computed = true, nickname = true)
        assertEquals(setOf("id", "first_name", "last_name", "street", "nickname"), columns(after))
        for (dialect in dialects()) {
            val desired = dialect.normalizeSchema(after)
            val operations = SchemaDiffPlanner.plan(desired, dialect.normalizeSchema(before))
            val operation = operations.single()
            assertTrue(operation is AddColumn, operations.toString())
            assertEquals("nickname", operation.column.name)
            val sql = dialect.render(operations).joinToString("\n")
            assertTrue(sql.contains("ALTER TABLE"), sql)
            assertComputedColumnsAbsent(sql)
            assertTrue(SchemaDiffPlanner.plan(desired, desired).isEmpty())
        }
    }

    private fun dialects() = listOf(PostgreSqlAutoDdlDialect(), MySqlAutoDdlDialect(), H2AutoDdlDialect())

    private fun columns(schema: AutoDdlSchema): Set<String> =
        requireNotNull(schema.table("computed_author")).columns.map { it.name }.toSet()

    private fun assertComputedColumnsAbsent(sql: String) {
        for (name in listOf("simple", "complex", "inherited", "sql_formula", "kotlin_formula", "transient_value", "display_street")) {
            assertFalse(sql.contains(name), sql)
        }
    }

    private fun compile(name: String, computed: Boolean, nickname: Boolean = false): AutoDdlSchema {
        val getters = if (computed) """
            val simple: String get() = firstName + " " + lastName
            val complex: String?
                get() {
                    val parts = listOf(firstName, lastName).filter { it.isNotBlank() }
                    return if (parts.isEmpty()) null else parts.joinToString(" ").uppercase()
                }
            @Formula(sql = "%alias.FIRST_NAME || ' ' || %alias.LAST_NAME")
            val sqlFormula: String
            @Formula(dependencies = ["firstName", "lastName"])
            val kotlinFormula: String get() = firstName + " " + lastName
            @Transient
            val transientValue: String
        """.trimIndent() else ""
        val inherited = if (computed) "val inherited: String get() = firstName.trim()" else ""
        val embedded = if (computed) "val displayStreet: String get() = street.uppercase()" else ""
        val stored = if (nickname) "val nickname: String?" else ""
        val source = SourceFile.kotlin("Author.kt", """
            package demo
            import org.babyfish.jimmer.Formula
            import org.babyfish.jimmer.sql.*
            @MappedSuperclass
            interface Base {
                val firstName: String
                val lastName: String
                $inherited
            }
            @Embeddable
            interface Address {
                val street: String
                $embedded
            }
            @Entity
            @Table(name = "computed_author")
            interface Author : Base {
                @Id val id: Long
                val address: Address
                $getters
                $stored
            }
        """.trimIndent())
        var schema: AutoDdlSchema? = null
        val messages = ByteArrayOutputStream()
        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    val entities = resolver.getSymbolsWithAnnotation("org.babyfish.jimmer.sql.Entity")
                        .filterIsInstance<KSClassDeclaration>()
                        .map { KspLsiClass(resolver, it) }
                        .toList()
                    if (entities.isNotEmpty()) {
                        schema = LsiAutoDdlSchemaAdapter.from(entities)
                    }
                    return emptyList()
                }
            }
        }
        val compilation = KotlinCompilation().apply {
            workingDir = temporaryFolder.newFolder(name)
            sources = listOf(source)
            inheritClassPath = true
            jvmTarget = "17"
            languageVersion = "2.1"
            apiVersion = "2.1"
            kotlincArguments = listOf("-Xskip-metadata-version-check")
            verbose = false
            messageOutputStream = messages
            useKsp2()
            symbolProcessorProviders = mutableListOf(provider)
        }
        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, messages.toString())
        return assertNotNull(schema)
    }
}
