package site.addzero.ddlgenerator.lsi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.lsi.support.TestAnnotation
import site.addzero.ddlgenerator.lsi.support.TestClass
import site.addzero.ddlgenerator.lsi.support.TestField
import site.addzero.ddlgenerator.lsi.support.TestType

class LsiAutoDdlSchemaAdapterTest {

    @Test
    fun `reads bean validation size max as explicit string length`() {
        val document = TestClass(
            simpleName = "Document",
            qualifiedName = "demo.Document",
            annotations = listOf(entity()),
            fields = listOf(
                TestField(
                    name = "title",
                    type = TestType("String"),
                    typeName = "String",
                    annotations = listOf(size(max = 128)),
                )
            ),
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(document))
        val title = schema.table("document")?.column("title")

        assertEquals(AutoDdlLogicalType.STRING, title?.logicalType)
        assertEquals(128, title?.length)
    }

    @Test
    fun `keeps Jimmer name enum on text fallback`() {
        val enumClass = TestClass(
            simpleName = "EnumKnowledgeType",
            qualifiedName = "demo.EnumKnowledgeType",
            annotations = listOf(enumType("NAME")),
            isEnum = true,
        )
        val document = TestClass(
            simpleName = "KnowledgeDocument",
            qualifiedName = "demo.KnowledgeDocument",
            annotations = listOf(entity()),
            fields = listOf(
                TestField(
                    name = "knowledgeType",
                    type = TestType("EnumKnowledgeType", lsiClass = enumClass),
                    typeName = "EnumKnowledgeType",
                    columnName = "knowledge_type",
                    fieldTypeClass = enumClass,
                    isEnum = true,
                )
            ),
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(document))

        assertEquals(AutoDdlLogicalType.UNKNOWN, schema.table("knowledge_document")?.column("knowledge_type")?.logicalType)
    }

    @Test
    fun `maps Jimmer ordinal enum to integer column`() {
        val enumClass = TestClass(
            simpleName = "EnumKnowledgeType",
            qualifiedName = "demo.EnumKnowledgeType",
            annotations = listOf(enumType("ORDINAL")),
            isEnum = true,
        )
        val document = TestClass(
            simpleName = "KnowledgeDocument",
            qualifiedName = "demo.KnowledgeDocument",
            annotations = listOf(entity()),
            fields = listOf(
                TestField(
                    name = "knowledgeType",
                    type = TestType("EnumKnowledgeType", lsiClass = enumClass),
                    typeName = "EnumKnowledgeType",
                    columnName = "knowledge_type",
                    fieldTypeClass = enumClass,
                    isEnum = true,
                )
            ),
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(document))

        assertEquals(AutoDdlLogicalType.INT32, schema.table("knowledge_document")?.column("knowledge_type")?.logicalType)
    }

    @Test
    fun `converts scalar columns foreign keys indexes and sequences`() {
        val customer = TestClass(
            simpleName = "Customer",
            qualifiedName = "demo.Customer",
            annotations = listOf(entity()),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id())
                ),
                TestField(
                    name = "code",
                    type = TestType("String"),
                    typeName = "String",
                    annotations = listOf(key(group = "biz")),
                    columnName = "customer_code",
                )
            )
        )
        val order = TestClass(
            simpleName = "OrderRecord",
            qualifiedName = "demo.OrderRecord",
            annotations = listOf(entity(), table(name = "order_record")),
            comment = "订单",
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id(), generatedValue(strategy = "SEQUENCE", generatorName = "order_seq"))
                ),
                TestField(
                    name = "amount",
                    type = TestType("BigDecimal"),
                    typeName = "BigDecimal",
                    annotations = listOf(column(precision = 10, scale = 2))
                ),
                TestField(
                    name = "customer",
                    type = TestType("Customer", qualifiedName = "demo.Customer", lsiClass = customer),
                    typeName = "Customer",
                    fieldTypeClass = customer,
                    annotations = listOf(manyToOne(), joinColumn("customer_id"))
                )
            )
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(order, customer))
        val orderTable = schema.table("order_record")
        assertNotNull(orderTable)
        assertEquals(3, orderTable.columns.size)
        assertEquals(AutoDdlLogicalType.DECIMAL, orderTable.column("amount")?.logicalType)
        assertEquals("customer_id", orderTable.column("customer_id")?.name)
        assertEquals("customer", orderTable.foreignKeys.single().referencedTableName)
        assertEquals("order_seq", schema.sequences.single().name)

        val customerTable = schema.table("customer")
        assertNotNull(customerTable)
        assertEquals("uk_customer_biz", customerTable.indexes.single().name)
    }

    @Test
    fun `skips unique index for nullable key fields`() {
        val user = TestClass(
            simpleName = "User",
            qualifiedName = "demo.User",
            annotations = listOf(entity(), table("system_users")),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id())
                ),
                TestField(
                    name = "username",
                    type = TestType("String"),
                    typeName = "String",
                    annotations = listOf(key(group = "username"))
                ),
                TestField(
                    name = "email",
                    type = TestType("String"),
                    typeName = "String",
                    annotations = listOf(key(group = "email")),
                    isNullable = true,
                )
            )
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(user))
        val table = schema.table("system_users")
        assertNotNull(table)

        assertEquals(listOf("uk_system_users_username"), table.indexes.map { index -> index.name })
    }

    @Test
    fun `converts joined inheritance into root and subtype physical tables`() {
        val root = TestClass(
            simpleName = "LogRecord",
            qualifiedName = "demo.LogRecord",
            annotations = listOf(entity(), table("system_log_record"), inheritance("JOINED")),
            fields = listOf(
                TestField(
                    name = "id",
                    type = TestType("Long"),
                    typeName = "Long",
                    annotations = listOf(id(), generatedValue(strategy = "IDENTITY")),
                ),
                TestField(
                    name = "recordType",
                    type = TestType("String"),
                    typeName = "String",
                    columnName = "record_type",
                ),
                TestField(
                    name = "userIp",
                    type = TestType("String"),
                    typeName = "String",
                    columnName = "user_ip",
                    isNullable = true,
                ),
            )
        )
        val operate = TestClass(
            simpleName = "OperateLog",
            qualifiedName = "demo.OperateLog",
            annotations = listOf(entity(), table("system_operate_log")),
            interfaces = listOf(root),
            fields = listOf(
                TestField(
                    name = "type",
                    type = TestType("String"),
                    typeName = "String",
                ),
                TestField(
                    name = "action",
                    type = TestType("String"),
                    typeName = "String",
                ),
            )
        )

        val schema = LsiAutoDdlSchemaAdapter.from(listOf(root, operate))

        val rootTable = schema.table("system_log_record")
        assertNotNull(rootTable)
        assertEquals(setOf("id", "record_type", "user_ip"), rootTable.columns.map { it.name }.toSet())
        assertEquals(true, rootTable.column("id")?.autoIncrement)

        val operateTable = schema.table("system_operate_log")
        assertNotNull(operateTable)
        assertEquals(setOf("id", "type", "action"), operateTable.columns.map { it.name }.toSet())
        assertEquals(true, operateTable.column("id")?.primaryKey)
        assertEquals(false, operateTable.column("id")?.autoIncrement)
        assertEquals("system_log_record", operateTable.foreignKeys.single().referencedTableName)
    }

    @Test
    fun `scans owning many to many into pure junction table`() {
        val role = TestClass(
            simpleName = "Role",
            qualifiedName = "demo.Role",
            annotations = listOf(entity()),
            fields = listOf(TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())))
        )
        val user = TestClass(
            simpleName = "UserAccount",
            qualifiedName = "demo.UserAccount",
            annotations = listOf(entity()),
            fields = listOf(
                TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())),
                TestField(
                    name = "roles",
                    type = TestType(
                        simpleName = "List",
                        qualifiedName = "kotlin.collections.List",
                        isCollectionType = true,
                        typeParameters = listOf(TestType("Role", qualifiedName = "demo.Role", lsiClass = role))
                    ),
                    typeName = "List",
                    isCollectionType = true,
                    annotations = listOf(
                        manyToMany(),
                        joinTable(name = "user_role", joinColumnName = "user_id", inverseJoinColumnName = "role_id")
                    )
                )
            )
        )

        val junctionTables = LsiAutoDdlSchemaAdapter.scanManyToManyTables(listOf(user, role))
        val junction = junctionTables.single()
        assertEquals("user_role", junction.name)
        assertEquals(listOf("user_id", "role_id"), junction.columns.map { it.name })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "user_account" })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "role" })
    }


    @Test
    fun `default many to many table name keeps DO suffix like Jimmer runtime`() {
        val user = TestClass(
            simpleName = "User",
            qualifiedName = "site.addzero.crud.model.system.user.User",
            annotations = listOf(entity(), table("system_users")),
            fields = listOf(TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())))
        )
        val project = TestClass(
            simpleName = "IotProjectDO",
            qualifiedName = "cn.iocoder.yudao.module.iot.project.IotProjectDO",
            annotations = listOf(entity(), table("iot_project")),
            fields = listOf(
                TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())),
                TestField(
                    name = "users",
                    type = TestType(
                        simpleName = "List",
                        qualifiedName = "kotlin.collections.List",
                        isCollectionType = true,
                        typeParameters = listOf(TestType("User", qualifiedName = "site.addzero.crud.model.system.user.User", lsiClass = user))
                    ),
                    typeName = "List",
                    isCollectionType = true,
                    annotations = listOf(manyToMany())
                )
            )
        )

        val junctionTables = LsiAutoDdlSchemaAdapter.scanManyToManyTables(listOf(project, user))
        val junction = junctionTables.single()
        assertEquals("iot_project_do_user_mapping", junction.name)
        assertEquals(listOf("iot_project_do_id", "user_id"), junction.columns.map { it.name })
    }

    @Test
    fun `scans inherited owning many to many when target entity comes from dependency module`() {
        val user = TestClass(
            simpleName = "User",
            qualifiedName = "site.addzero.crud.model.system.user.User",
            annotations = listOf(entity(), table("system_users")),
            fields = listOf(TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())))
        )
        val basePersonInCharge = TestClass(
            simpleName = "BasePersonInCharge",
            qualifiedName = "cn.iocoder.yudao.module.ai.power.equipment_information_archive.entity.BasePersonInCharge",
            annotations = listOf(mappedSuperclass()),
            fields = listOf(
                TestField(
                    name = "personInCharge",
                    type = TestType(
                        simpleName = "List",
                        qualifiedName = "kotlin.collections.List",
                        isCollectionType = true,
                        typeParameters = listOf(
                            TestType(
                                simpleName = "User",
                                qualifiedName = "site.addzero.crud.model.system.user.User",
                                lsiClass = user,
                            )
                        )
                    ),
                    typeName = "List",
                    isCollectionType = true,
                    annotations = listOf(manyToMany())
                )
            )
        )
        val device = TestClass(
            simpleName = "EquipmentInformationArchive",
            qualifiedName = "cn.iocoder.yudao.module.ai.power.equipment_information_archive.entity.EquipmentInformationArchive",
            annotations = listOf(entity(), table("ai_power_device")),
            fields = listOf(TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id()))),
            interfaces = listOf(basePersonInCharge),
        )

        val baseSchema = LsiAutoDdlSchemaAdapter.from(listOf(device))
        val junctionTables = LsiAutoDdlSchemaAdapter.scanManyToManyTables(
            owningClasses = listOf(device),
            targetClasses = listOf(user),
        )

        assertEquals(1, baseSchema.tables.size)
        assertEquals("ai_power_device", baseSchema.tables.single().name)
        val junction = junctionTables.single()
        assertEquals("equipment_information_archive_person_in_charge_mapping", junction.name)
        assertEquals(listOf("equipment_information_archive_id", "user_id"), junction.columns.map { it.name })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "ai_power_device" })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "system_users" })
    }

    @Test
    fun `scans mapped by many to many using jimmer default mapping table name`() {
        val user = TestClass(
            simpleName = "User",
            qualifiedName = "site.addzero.crud.model.system.user.User",
            annotations = listOf(entity(), table("system_users")),
            fields = listOf(TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())))
        )
        val dept = TestClass(
            simpleName = "Dept",
            qualifiedName = "site.addzero.crud.model.system.dept.Dept",
            annotations = listOf(entity(), table("system_dept")),
            fields = listOf(
                TestField(name = "id", type = TestType("Long"), typeName = "Long", annotations = listOf(id())),
                TestField(
                    name = "users",
                    type = TestType(
                        simpleName = "List",
                        qualifiedName = "kotlin.collections.List",
                        isCollectionType = true,
                        typeParameters = listOf(
                            TestType(
                                simpleName = "User",
                                qualifiedName = "site.addzero.crud.model.system.user.User",
                                lsiClass = user,
                            )
                        )
                    ),
                    typeName = "List",
                    isCollectionType = true,
                    annotations = listOf(manyToMany(mappedBy = "depts"))
                )
            )
        )

        val junctionTables = LsiAutoDdlSchemaAdapter.scanManyToManyTables(listOf(user, dept))

        val junction = junctionTables.single()
        assertEquals("user_dept_mapping", junction.name)
        assertEquals(listOf("user_id", "dept_id"), junction.columns.map { it.name })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "system_users" })
        assertTrue(junction.foreignKeys.any { it.referencedTableName == "system_dept" })
    }

    private fun entity(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Entity", "Entity")
    }

    private fun enumType(strategy: String): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.EnumType", "EnumType", mapOf("value" to strategy))
    }

    private fun mappedSuperclass(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.MappedSuperclass", "MappedSuperclass")
    }

    private fun table(name: String): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Table", "Table", mapOf("name" to name))
    }

    private fun id(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Id", "Id")
    }

    private fun key(group: String? = null): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Key", "Key", buildMap {
            if (group != null) {
                put("group", group)
            }
        })
    }

    private fun size(max: Int): TestAnnotation {
        return TestAnnotation("jakarta.validation.constraints.Size", "Size", mapOf("max" to max))
    }

    private fun column(
        precision: Int? = null,
        scale: Int? = null,
    ): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Column", "Column", buildMap {
            if (precision != null) {
                put("precision", precision)
            }
            if (scale != null) {
                put("scale", scale)
            }
        })
    }

    private fun generatedValue(
        strategy: String,
        generatorName: String = "",
        sequenceName: String = generatorName,
        generatorType: String? = null,
        generatorRef: String? = null,
    ): TestAnnotation {
        return TestAnnotation(
            "org.babyfish.jimmer.sql.GeneratedValue",
            "GeneratedValue",
            buildMap {
                put("strategy", strategy)
                if (generatorName.isNotBlank()) {
                    put("generatorName", generatorName)
                }
                if (sequenceName.isNotBlank()) {
                    put("sequenceName", sequenceName)
                }
                if (generatorType != null) {
                    put("generatorType", generatorType)
                }
                if (generatorRef != null) {
                    put("generatorRef", generatorRef)
                }
            }
        )
    }

    private fun inheritance(strategy: String): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.Inheritance", "Inheritance", mapOf("strategy" to strategy))
    }

    private fun manyToOne(): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.ManyToOne", "ManyToOne")
    }

    private fun joinColumn(name: String): TestAnnotation {
        return TestAnnotation(
            "org.babyfish.jimmer.sql.JoinColumn",
            "JoinColumn",
            mapOf("name" to name, "referencedColumnName" to "id")
        )
    }

    private fun manyToMany(mappedBy: String? = null): TestAnnotation {
        return TestAnnotation("org.babyfish.jimmer.sql.ManyToMany", "ManyToMany", buildMap {
            if (mappedBy != null) {
                put("mappedBy", mappedBy)
            }
        })
    }

    private fun joinTable(
        name: String,
        joinColumnName: String,
        inverseJoinColumnName: String,
    ): TestAnnotation {
        return TestAnnotation(
            "org.babyfish.jimmer.sql.JoinTable",
            "JoinTable",
            mapOf(
                "name" to name,
                "joinColumnName" to joinColumnName,
                "inverseJoinColumnName" to inverseJoinColumnName,
            )
        )
    }
}
