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
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.assertFailsWith
import java.nio.file.Files
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import site.addzero.ddlgenerator.core.diff.SchemaDiffPlanner
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.options.AutoDdlDiffOptions
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
class AssociationKeyDdlTest {
    @Test
    fun `many to one key includes default snake case foreign key column`() {
        val table = table("membership")
        assertEquals(listOf("owner_account_id", "code"), table.indexes.single().columnNames)
        assertEquals(listOf("owner_account_id"), table.foreignKeys.single().columnNames)
        assertEquals(listOf("account_pk"), table.foreignKeys.single().referencedColumnNames)
    }

    @Test
    fun `custom join column participates in every repeated key group`() {
        val table = table("grouped_membership")
        assertEquals(
            mapOf("uk_grouped_membership_code" to listOf("account_fk", "code"), "uk_grouped_membership_alias" to listOf("account_fk", "alias")),
            table.indexes.associate { it.name to it.columnNames },
        )
        assertEquals(listOf("account_fk"), table.foreignKeys.single().columnNames)
    }

    @Test
    fun `nullable association does not shrink composite key into a scalar unique constraint`() {
        assertTrue(table("optional_membership").indexes.isEmpty())
        assertTrue(table("optional_membership").column("owner_account_id")!!.nullable)
    }

    @Test
    fun `many to one without key remains non unique and fake foreign key retains key`() {
        assertTrue(table("ordinary_membership").indexes.isEmpty())
        assertEquals(listOf("account_fk"), table("fake_membership").indexes.single().columnNames)
        assertTrue(table("fake_membership").foreignKeys.isEmpty())
    }

    @Test
    fun `owning one to one is unique while inverse side has no stored column`() {
        assertEquals(listOf("account_fk"), table("profile").indexes.single().columnNames)
        assertEquals(listOf("account_fk"), table("profile").foreignKeys.single().columnNames)
        assertEquals(setOf("account_pk"), table("account").columns.map { it.name }.toSet())
        assertTrue(table("account").foreignKeys.isEmpty())
        assertTrue(table("profile").column("account_fk")!!.nullable)
    }

    @Test
    fun `one to one composite key keeps both natural and relationship uniqueness`() {
        val columns = table("keyed_profile").indexes.map { it.columnNames }.toSet()
        assertEquals(setOf(listOf("account_fk", "code"), listOf("account_fk")), columns)
    }

    @Test
    fun `composite foreign key uses all id columns in target order and unique key`() {
        val table = table("composite_member")
        assertEquals(listOf("tenant_fk", "number_fk", "code"), table.indexes.single().columnNames)
        assertEquals(listOf("tenant_fk", "number_fk"), table.foreignKeys.single().columnNames)
        assertEquals(listOf("tenant_id", "account_number"), table.foreignKeys.single().referencedColumnNames)
        assertEquals(listOf("tenant_id", "account_number"), table("composite_account").primaryKeyColumnNames)
    }

    @Test
    fun `dialects render complete keys and repair old scalar only key without oscillation`() {
        val before = schema.copy(tables = schema.tables.map { table ->
            if (table.name == "membership") table.copy(indexes = table.indexes.map {
                it.copy(name = "uk_membership_code", columnNames = listOf("code"))
            }) else table
        })
        for (dialect in listOf(PostgreSqlAutoDdlDialect(), MySqlAutoDdlDialect(), H2AutoDdlDialect())) {
            val desired = dialect.normalizeSchema(schema)
            val createSql = dialect.render(SchemaDiffPlanner.plan(desired, AutoDdlSchema(emptyList()))).joinToString("\n")
            assertTrue(createSql.contains("owner_account_id"), createSql)
            assertFalse(createSql.contains("ownerAccount"), createSql)
            val repair = dialect.render(SchemaDiffPlanner.plan(desired, dialect.normalizeSchema(before), AutoDdlDiffOptions(allowDestructiveChanges = true))).joinToString("\n")
            assertTrue(repair.contains("DROP"), repair)
            assertTrue(repair.contains("owner_account_id"), repair)
            assertTrue(SchemaDiffPlanner.plan(desired, desired).isEmpty())
        }
    }

    @Test
    fun `H2 executes generated composite foreign keys and rejects duplicate association keys`() {
        val dialect = H2AutoDdlDialect()
        val normalized = dialect.normalizeSchema(schema)
        val statements = dialect.render(SchemaDiffPlanner.plan(normalized, AutoDdlSchema(emptyList())))
        DriverManager.getConnection("jdbc:h2:mem:association_ddl;DATABASE_TO_LOWER=TRUE").use { connection ->
            connection.createStatement().use { statement ->
                statements.forEach { statement.execute(it) }
                statement.execute("insert into account(account_pk) values (1), (2)")
                statement.execute("insert into membership(id, owner_account_id, code) values (1, 1, 'A'), (2, 2, 'A')")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into membership(id, owner_account_id, code) values (3, 1, 'A')")
                }.sqlState)
                statement.execute("insert into composite_account(tenant_id, account_number) values (1, 10), (2, 10)")
                statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (1, 1, 10, 'A'), (2, 2, 10, 'A')")
                assertEquals("23505", assertFailsWith<SQLException> {
                    statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (3, 1, 10, 'A')")
                }.sqlState)
                assertEquals("23506", assertFailsWith<SQLException> {
                    statement.execute("insert into composite_member(id, tenant_fk, number_fk, code) values (4, 1, 99, 'B')")
                }.sqlState)
            }
        }
    }

    private fun table(name: String) = assertNotNull(schema.table(name))

    companion object {
        private val schema: AutoDdlSchema by lazy { compileSchema() }

        private fun compileSchema(): AutoDdlSchema {
            val declarations = listOf(
                """
                @Entity @Table(name = "account") interface Account {
                    @Id @Column(name = "account_pk") val id: Long
                    @OneToOne(mappedBy = "account") val profile: Profile?
                }
                """,
                """
                @Entity @Table(name = "membership") interface Membership {
                    @Id val id: Long
                    @Key @ManyToOne val ownerAccount: Account
                    @Key val code: String
                }
                """,
                """
                @Entity @Table(name = "grouped_membership") interface GroupedMembership {
                    @Id val id: Long
                    @Key(group = "code") @Key(group = "alias")
                    @ManyToOne @JoinColumn(name = "account_fk") val ownerAccount: Account
                    @Key(group = "code") val code: String
                    @Key(group = "alias") val alias: String
                }
                """,
                """
                @Entity @Table(name = "optional_membership") interface OptionalMembership {
                    @Id val id: Long
                    @Key @ManyToOne val ownerAccount: Account?
                    @Key val code: String
                }
                """,
                """
                @Entity @Table(name = "ordinary_membership") interface OrdinaryMembership {
                    @Id val id: Long
                    @ManyToOne val ownerAccount: Account
                }
                """,
                """
                @Entity @Table(name = "fake_membership") interface FakeMembership {
                    @Id val id: Long
                    @Key @ManyToOne(inputNotNull = true)
                    @JoinColumn(name = "account_fk", foreignKeyType = ForeignKeyType.FAKE) val ownerAccount: Account?
                }
                """,
                """
                @Entity @Table(name = "profile") interface Profile {
                    @Id val id: Long
                    @OneToOne @JoinColumn(name = "account_fk") val account: Account?
                }
                """,
                """
                @Entity @Table(name = "keyed_profile") interface KeyedProfile {
                    @Id val id: Long
                    @Key(group = "account_fk") @OneToOne @JoinColumn(name = "account_fk") val account: Account
                    @Key(group = "account_fk") val code: String
                }
                """,
                """
                @Embeddable interface AccountId {
                    @Column(name = "tenant_id") val tenant: Long
                    @Column(name = "account_number") val number: Int
                }
                """,
                """
                @Entity @Table(name = "composite_account") interface CompositeAccount {
                    @Id val id: AccountId
                }
                """,
                """
                @Entity @Table(name = "composite_member") interface CompositeMember {
                    @Id val id: Long
                    @Key @ManyToOne
                    @JoinColumn(name = "number_fk", referencedColumnName = "account_number")
                    @JoinColumn(name = "tenant_fk", referencedColumnName = "tenant_id", foreignKeyType = ForeignKeyType.REAL)
                    val account: CompositeAccount
                    @Key val code: String
                }
                """,
            )
            var captured: AutoDdlSchema? = null
            val messages = ByteArrayOutputStream()
            val provider = SymbolProcessorProvider {
                object : SymbolProcessor {
                    override fun process(resolver: Resolver): List<KSAnnotated> {
                        val classes = resolver.getSymbolsWithAnnotation("org.babyfish.jimmer.sql.Entity")
                            .filterIsInstance<KSClassDeclaration>().map { KspLsiClass(resolver, it) }.toList()
                        if (classes.isNotEmpty()) {
                            captured = LsiAutoDdlSchemaAdapter.from(classes)
                        }
                        return emptyList()
                    }
                }
            }
            val directory = Files.createTempDirectory("association-key-ksp").toFile()
            try {
                val result = KotlinCompilation().apply {
                    workingDir = directory
                    sources = declarations.mapIndexed { index, declaration ->
                        SourceFile.kotlin("Entity$index.kt", "package demo\nimport org.babyfish.jimmer.sql.*\n" + declaration.trimIndent())
                    }
                    inheritClassPath = true
                    jvmTarget = "17"
                    languageVersion = "2.1"
                    apiVersion = "2.1"
                    kotlincArguments = listOf("-Xskip-metadata-version-check")
                    verbose = false
                    messageOutputStream = messages
                    useKsp2()
                    symbolProcessorProviders = mutableListOf(provider)
                }.compile()
                assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, messages.toString())
                return assertNotNull(captured)
            } finally {
                directory.deleteRecursively()
            }
        }
    }
}
