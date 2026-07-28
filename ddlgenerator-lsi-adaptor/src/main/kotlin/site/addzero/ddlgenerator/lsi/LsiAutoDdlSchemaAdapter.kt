package site.addzero.ddlgenerator.lsi

import site.addzero.ddlgenerator.core.model.AutoDdlColumn
import site.addzero.ddlgenerator.core.model.AutoDdlForeignKey
import site.addzero.ddlgenerator.core.model.AutoDdlIndex
import site.addzero.ddlgenerator.core.model.AutoDdlIndexType
import site.addzero.ddlgenerator.core.model.AutoDdlJunction
import site.addzero.ddlgenerator.core.model.AutoDdlLogicalType
import site.addzero.ddlgenerator.core.model.AutoDdlSchema
import site.addzero.ddlgenerator.core.model.AutoDdlSequence
import site.addzero.ddlgenerator.core.model.AutoDdlTable
import site.addzero.lsi.anno.LsiAnnotation
import site.addzero.lsi.clazz.LsiClass
import site.addzero.lsi.clazz.guessTableName
import site.addzero.lsi.field.LsiField

object LsiAutoDdlSchemaAdapter {
    fun from(classes: List<LsiClass>): AutoDdlSchema {
        val entities = classes.filter { it.isPersistedEntity() }
        val tables = entities.map { entity ->
            entity.toAutoDdlTable(entities)
        }
        val sequences = entities
            .mapNotNull { entity ->
                entity.allFields()
                    .firstOrNull { it.isIdField() && !it.sequenceName().isNullOrBlank() }
                    ?.sequenceName()
            }
            .distinct()
            .map { AutoDdlSequence(name = it) }
        return AutoDdlSchema(tables = tables, sequences = sequences)
    }

    private fun LsiClass.toAutoDdlTable(allEntities: List<LsiClass>): AutoDdlTable {
        val owner = this
        val joinedRoot = joinedInheritanceRoot()
            ?.takeUnless { root -> root.isSameType(owner) }
        val rootIdField = joinedRoot?.allFields()?.firstOrNull { field -> field.isIdField() }
        val tableFields = tableFieldsForPhysicalTable()
        val scalarColumns = mutableListOf<AutoDdlColumn>()
        val foreignKeys = mutableListOf<AutoDdlForeignKey>()

        tableFields.forEach { field ->
            when {
                field.shouldSkipField() -> Unit
                field.isOwningAssociation() -> {
                    val referencedClass = field.resolveAssociationTargetClass(owner, allEntities) ?: return@forEach
                    val referencedId = referencedClass.allFields().firstOrNull { it.isIdField() }
                    val columnName = field.joinColumnName()?.takeIf(String::isNotBlank) ?: "${field.name.orEmpty()}_id"
                    val referenceColumnName = field.referencedColumnName()?.takeIf(String::isNotBlank)
                        ?: referencedId?.columnName?.takeIf(String::isNotBlank)
                        ?: referencedId?.name?.takeIf(String::isNotBlank)
                        ?: "id"
                    val column = AutoDdlColumn(
                        name = columnName,
                        logicalType = referencedId?.toLogicalType() ?: AutoDdlLogicalType.INT64,
                        nullable = field.isNullable,
                        comment = field.comment,
                    )
                    scalarColumns += column
                    if (!field.isFakeForeignKey()) {
                        foreignKeys += AutoDdlForeignKey(
                            name = "fk_${guessTableName}_$columnName",
                            columnNames = listOf(columnName),
                            referencedTableName = referencedClass.guessTableName,
                            referencedColumnNames = listOf(referenceColumnName),
                        )
                    }
                }
                field.isOwningManyToMany() -> Unit
                else -> {
                    val column = field.toColumn()
                    scalarColumns += if (joinedRoot != null && field.isSameField(rootIdField)) {
                        column.copy(autoIncrement = false, sequenceName = null)
                    } else {
                        column
                    }
                }
            }
        }
        if (joinedRoot != null && rootIdField != null) {
            val idColumnName = rootIdField.columnName ?: rootIdField.name ?: "id"
            foreignKeys += AutoDdlForeignKey(
                name = "fk_${guessTableName}_$idColumnName",
                columnNames = listOf(idColumnName),
                referencedTableName = joinedRoot.guessTableName,
                referencedColumnNames = listOf(idColumnName),
            )
        }

        val indexes = buildIndexes(this, scalarColumns, tableFields)
        return AutoDdlTable(
            name = guessTableName,
            comment = comment,
            columns = scalarColumns.distinctBy { it.name.lowercase() },
            indexes = indexes,
            foreignKeys = foreignKeys,
        )
    }

    private fun buildIndexes(
        clazz: LsiClass,
        columns: List<AutoDdlColumn>,
        tableFields: List<LsiField> = clazz.allFields(),
    ): List<AutoDdlIndex> {
        val fields = tableFields
            .filter { field -> columns.any { it.name.equals(field.columnName ?: field.name, ignoreCase = true) } }

        val groupedKeys = fields
            .filter { it.hasAnnotationSimple("Key") && !it.isIdField() }
            .groupBy { it.annotationValue("Key", "group")?.takeIf(String::isNotBlank) ?: "" }

        return buildList {
            groupedKeys.forEach { (groupName, groupedFields) ->
                if (groupedFields.any { field -> field.isNullable }) {
                    return@forEach
                }
                val columnNames = groupedFields.mapNotNull { it.columnName ?: it.name }
                if (columnNames.isEmpty()) {
                    return@forEach
                }
                val normalizedTableName = clazz.guessTableName
                val indexName = if (groupName.isBlank()) {
                    "uk_${normalizedTableName}_${columnNames.joinToString("_")}"
                } else {
                    "uk_${normalizedTableName}_$groupName"
                }
                add(
                    AutoDdlIndex(
                        name = indexName,
                        columnNames = columnNames,
                        type = AutoDdlIndexType.UNIQUE,
                    )
                )
            }

            fields.filter { it.isUniqueField() && !it.hasAnnotationSimple("Key") }
                .forEach { field ->
                    val columnName = field.columnName ?: field.name ?: return@forEach
                    add(
                        AutoDdlIndex(
                            name = "uk_${clazz.guessTableName}_$columnName",
                            columnNames = listOf(columnName),
                            type = AutoDdlIndexType.UNIQUE,
                        )
                    )
                }
        }.distinctBy { it.name.lowercase() }
    }

    fun scanManyToManyTables(classes: List<LsiClass>): List<AutoDdlTable> {
        return scanManyToManyTables(
            owningClasses = classes,
            targetClasses = classes,
        )
    }

    fun scanManyToManyTables(
        owningClasses: List<LsiClass>,
        targetClasses: List<LsiClass>,
    ): List<AutoDdlTable> {
        val owners = owningClasses.filter { it.isPersistedEntity() }
        val entities = (owningClasses + targetClasses)
            .filter { it.isPersistedEntity() }
            .distinctBy { it.qualifiedName ?: it.simpleName.orEmpty() }
        val tables = linkedMapOf<String, AutoDdlTable>()
        owners.forEach { leftEntity ->
            leftEntity.allFields()
                .filter { it.hasAnnotationSimple("ManyToMany") }
                .forEach { field ->
                    val rightEntity = field.resolveManyToManyTarget(entities) ?: return@forEach
                    val mappedBy = field.annotationValue("ManyToMany", "mappedBy")
                        ?.takeIf { it.isNotBlank() }
                    val ownerEntity = if (mappedBy == null) leftEntity else rightEntity
                    val inverseEntity = if (mappedBy == null) rightEntity else leftEntity
                    val relationName = mappedBy ?: field.name.orEmpty()
                    val tableName = field.annotationValue("JoinTable", "name")
                        ?.takeIf { it.isNotBlank() }
                        ?: "${ownerEntity.jimmerAssociationToken()}_${relationName.toJimmerAssociationToken()}_mapping"
                    val leftColumnName = field.annotationValue("JoinTable", "joinColumnName")
                        ?.takeIf { it.isNotBlank() }
                        ?: "${ownerEntity.jimmerAssociationToken()}_id"
                    val rightColumnName = field.annotationValue("JoinTable", "inverseJoinColumnName")
                        ?.takeIf { it.isNotBlank() }
                        ?: "${inverseEntity.jimmerAssociationToken()}_id"

                    val filterColumns = field.readJoinTableFilterColumns()
                    val table = AutoDdlTable(
                        name = tableName,
                        columns = listOf(
                            AutoDdlColumn(leftColumnName, AutoDdlLogicalType.INT64, nullable = false, primaryKey = true),
                            AutoDdlColumn(rightColumnName, AutoDdlLogicalType.INT64, nullable = false, primaryKey = true),
                        ) + filterColumns,
                        foreignKeys = listOf(
                            AutoDdlForeignKey(
                                name = "fk_${tableName}_$leftColumnName",
                                columnNames = listOf(leftColumnName),
                                referencedTableName = ownerEntity.guessTableName,
                                referencedColumnNames = listOf("id"),
                            ),
                            AutoDdlForeignKey(
                                name = "fk_${tableName}_$rightColumnName",
                                columnNames = listOf(rightColumnName),
                                referencedTableName = inverseEntity.guessTableName,
                                referencedColumnNames = listOf("id"),
                            )
                        ),
                        junction = AutoDdlJunction(
                            leftTableName = ownerEntity.guessTableName,
                            rightTableName = inverseEntity.guessTableName,
                            leftColumnName = leftColumnName,
                            rightColumnName = rightColumnName,
                        )
                    )
                    val existingTable = tables[table.name.lowercase()]
                    if (existingTable != null) {
                        val mergedColumns = (existingTable.columns + table.columns).distinctBy { it.name.lowercase() }
                        tables[table.name.lowercase()] = existingTable.copy(columns = mergedColumns)
                    } else {
                        tables[table.name.lowercase()] = table
                    }
                }
        }
        return tables.values.toList()
    }

    private fun LsiClass.jimmerAssociationToken(): String {
        val name = simpleName.orEmpty()
            .removeSuffix("Entity")
        return name.toJimmerSnakeCase()
    }

    private fun String.toJimmerAssociationToken(): String {
        val snakeName = toJimmerSnakeCase()
        return when {
            snakeName.endsWith("ies") -> snakeName.dropLast(3) + "y"
            snakeName.endsWith("s") && !snakeName.endsWith("ss") -> snakeName.dropLast(1)
            else -> snakeName
        }
    }

    private fun String.toJimmerSnakeCase(): String {
        return buildString {
            this@toJimmerSnakeCase.forEachIndexed { index, char ->
                val previous = this@toJimmerSnakeCase.getOrNull(index - 1)
                val next = this@toJimmerSnakeCase.getOrNull(index + 1)
                when {
                    char == '-' || char == '.' || char == ' ' -> append('_')
                    char.isUpperCase() -> {
                        val shouldSplit = index > 0 && lastOrNull() != '_' &&
                            (previous?.isLowerCase() == true || previous?.isDigit() == true || next?.isLowerCase() == true)
                        if (shouldSplit) {
                            append('_')
                        }
                        append(char.lowercaseChar())
                    }
                    else -> append(char)
                }
            }
        }.replace(Regex("_+"), "_").trim('_')
    }

    private fun LsiClass.allFields(): List<LsiField> {
        return allFields(visited = linkedSetOf())
    }

    private fun LsiClass.tableFieldsForPhysicalTable(): List<LsiField> {
        val joinedRoot = joinedInheritanceRoot()
        if (joinedRoot == null || joinedRoot.isSameType(this)) {
            return allFields()
        }
        val rootIdField = joinedRoot.allFields().firstOrNull { field -> field.isIdField() }
        val inheritedFieldNames = (superClasses + interfaces)
            .flatMap { parent -> parent.allFields() }
            .mapNotNull { field -> field.name }
            .toSet()
        val declaredBranchFields = fields.filter { field ->
            field.name !in inheritedFieldNames
        }
        return (listOfNotNull(rootIdField) + declaredBranchFields)
            .distinctBy { field -> field.name }
    }

    private fun LsiClass.joinedInheritanceRoot(): LsiClass? {
        if (isJoinedInheritanceRoot()) {
            return this
        }
        return (superClasses + interfaces)
            .firstNotNullOfOrNull { parent -> parent.joinedInheritanceRoot() }
    }

    private fun LsiClass.isJoinedInheritanceRoot(): Boolean {
        val inheritance = annotation("Inheritance") ?: return false
        val strategy = inheritance.getAttribute("strategy")?.toString().orEmpty()
        return strategy.isBlank() ||
            strategy.endsWith("JOINED", ignoreCase = true) ||
            strategy.equals("JOINED", ignoreCase = true)
    }

    private fun LsiClass.isSameType(other: LsiClass?): Boolean {
        if (other == null) {
            return false
        }
        val qualifiedName = this.qualifiedName
        val otherQualifiedName = other.qualifiedName
        if (!qualifiedName.isNullOrBlank() && qualifiedName == otherQualifiedName) {
            return true
        }
        return simpleName == other.simpleName
    }

    private fun LsiField.isSameField(other: LsiField?): Boolean {
        if (other == null) {
            return false
        }
        if (this == other) {
            return true
        }
        return name == other.name && declaringClass?.isSameType(other.declaringClass) == true
    }

    private fun LsiClass.allFields(visited: MutableSet<String>): List<LsiField> {
        val key = qualifiedName ?: simpleName.orEmpty()
        if (key.isNotBlank() && !visited.add(key)) {
            return emptyList()
        }
        val inheritedFields = (superClasses + interfaces)
            .flatMap { parent -> parent.allFields(visited) }
        return (inheritedFields + fields).distinctBy { it.name }
    }

    private fun LsiClass.implementsInterface(
        qualifiedName: String,
        simpleName: String,
    ): Boolean {
        return interfaces.any { item ->
            item.qualifiedName == qualifiedName ||
                item.simpleName == simpleName ||
                item.implementsInterface(qualifiedName, simpleName)
        }
    }

    private fun LsiClass.isPersistedEntity(): Boolean {
        return annotations.any { annotation ->
            annotation.qualifiedName in ENTITY_ANNOTATIONS
        }
    }

    private fun LsiField.toColumn(): AutoDdlColumn {
        val columnName = columnName ?: name.orEmpty()
        return AutoDdlColumn(
            name = columnName,
            logicalType = toLogicalType(),
            nullable = isNullable,
            length = length(),
            precision = precision(),
            scale = scale(),
            defaultValue = defaultValue?.takeIf { it.isNotBlank() },
            comment = comment,
            primaryKey = isIdField(),
            autoIncrement = isAutoIncrement(),
            sequenceName = sequenceName(),
            nativeTypeHint = nativeTypeHint(),
        )
    }

    private fun LsiField.toLogicalType(): AutoDdlLogicalType {
        if (isJsonType()) {
            return AutoDdlLogicalType.JSON
        }
        enumLogicalType()?.let { logicalType ->
            return logicalType
        }
        val rawType = typeName?.substringAfterLast('.') ?: return AutoDdlLogicalType.UNKNOWN
        return when (rawType) {
            "String" -> if (isTextType()) AutoDdlLogicalType.TEXT else AutoDdlLogicalType.STRING
            "Char", "Character" -> AutoDdlLogicalType.CHAR
            "Boolean", "boolean" -> AutoDdlLogicalType.BOOLEAN
            "Byte", "byte" -> AutoDdlLogicalType.INT8
            "Short", "short" -> AutoDdlLogicalType.INT16
            "Int", "Integer", "int" -> AutoDdlLogicalType.INT32
            "Long", "long" -> AutoDdlLogicalType.INT64
            "Float", "float" -> AutoDdlLogicalType.FLOAT32
            "Double", "double" -> AutoDdlLogicalType.FLOAT64
            "BigDecimal" -> AutoDdlLogicalType.DECIMAL
            "BigInteger" -> AutoDdlLogicalType.BIG_INTEGER
            "LocalDate", "sqlDate", "DateOnly" -> AutoDdlLogicalType.DATE
            "LocalTime", "sqlTime" -> AutoDdlLogicalType.TIME
            "Instant", "OffsetDateTime", "ZonedDateTime" -> AutoDdlLogicalType.DATETIME_TZ
            "LocalDateTime", "Date", "sqlTimestamp", "Timestamp" -> AutoDdlLogicalType.DATETIME
            "Duration" -> AutoDdlLogicalType.DURATION
            "UUID" -> AutoDdlLogicalType.UUID
            "JsonNode" -> AutoDdlLogicalType.JSON
            "ByteArray", "byte[]" -> AutoDdlLogicalType.BINARY
            else -> AutoDdlLogicalType.UNKNOWN
        }
    }

    private fun LsiField.enumLogicalType(): AutoDdlLogicalType? {
        if (!isEnum && fieldTypeClass?.isEnum != true) {
            return null
        }
        val strategy = fieldTypeClass
            ?.annotation("EnumType")
            ?.getAttribute("value")
            .enumConstantName()
        return when (strategy) {
            "ORDINAL" -> AutoDdlLogicalType.INT32
            else -> null
        }
    }

    private fun Any?.enumConstantName(): String? {
        return this
            ?.toString()
            ?.trim()
            ?.substringAfterLast('.')
            ?.substringAfterLast('$')
            ?.uppercase()
    }

    private fun LsiField.shouldSkipField(): Boolean {
        return isStatic ||
            hasAnnotationSimple("Transient", "Formula", "ManyToManyView", "IdView") ||
            (isCollectionType && !isOwningManyToMany() && !isSerializedScalar())
    }

    private fun LsiField.isOwningAssociation(): Boolean {
        if (!hasAnnotationSimple("ManyToOne", "OneToOne")) {
            return false
        }
        return annotationValue("ManyToOne", "mappedBy").isNullOrBlank() &&
            annotationValue("OneToOne", "mappedBy").isNullOrBlank()
    }

    private fun LsiField.resolveAssociationTargetClass(
        owner: LsiClass,
        allEntities: List<LsiClass>,
    ): LsiClass? {
        fieldTypeClass
            ?.takeIf { target -> target.isPersistedEntity() }
            ?.let { return it }

        val typeNames = listOfNotNull(type?.qualifiedName, typeName, type?.presentableText)
            .map { value -> value.trim().removeSuffix("?") }
            .filter { value -> value.isNotBlank() }
        allEntities.firstOrNull { entity ->
            typeNames.any { typeName ->
                typeName == entity.qualifiedName ||
                    typeName == entity.simpleName ||
                    typeName.endsWith(".${entity.simpleName}")
            }
        }?.let { return it }

        if (
            name == "parent" &&
            owner.implementsInterface(
                qualifiedName = "site.addzero.crud.model.BaseTreeNode",
                simpleName = "BaseTreeNode",
            )
        ) {
            return owner
        }

        return null
    }

    private fun LsiField.isFakeForeignKey(): Boolean {
        val foreignKeyType = annotationValue("JoinColumn", "foreignKeyType") ?: return false
        return foreignKeyType.endsWith("FAKE", ignoreCase = true)
    }

    private fun LsiField.isOwningManyToMany(): Boolean {
        if (!hasAnnotationSimple("ManyToMany")) {
            return false
        }
        return annotationValue("ManyToMany", "mappedBy").isNullOrBlank()
    }

    private fun LsiField.resolveManyToManyTarget(allEntities: List<LsiClass>): LsiClass? {
        val typeParameters = type?.typeParameters.orEmpty()
        typeParameters.firstOrNull()
            ?.lsiClass
            ?.takeIf { target -> target.isPersistedEntity() }
            ?.let { return it }
        fieldTypeClass
            ?.takeIf { target -> target.isPersistedEntity() }
            ?.let { return it }
        val targetType = typeParameters.firstOrNull()?.qualifiedName ?: fieldTypeClass?.qualifiedName
        return allEntities.firstOrNull { entity ->
            entity.qualifiedName == targetType || entity.simpleName == targetType
        }
    }

    private fun LsiField.isIdField(): Boolean {
        return hasAnnotationSimple("Id") || name.equals("id", ignoreCase = true)
    }

    private fun LsiField.isAutoIncrement(): Boolean {
        if (!hasAnnotationSimple("GeneratedValue")) {
            return false
        }
        if (hasCustomIdGenerator()) {
            return false
        }
        val strategy = annotationValue("GeneratedValue", "strategy")
        return strategy.isNullOrBlank() || strategy.contains("IDENTITY", ignoreCase = true) || strategy.contains("AUTO", ignoreCase = true)
    }

    private fun LsiField.hasCustomIdGenerator(): Boolean {
        val generatorRef = annotationValue("GeneratedValue", "generatorRef")
        if (!generatorRef.isNullOrBlank()) {
            return true
        }
        val generatorType = annotationValue("GeneratedValue", "generatorType") ?: return false
        if (generatorType.isBlank()) {
            return false
        }
        return !generatorType.contains("UserIdGenerator.None") &&
            !generatorType.endsWith(".None") &&
            !generatorType.endsWith("$" + "None") &&
            !generatorType.endsWith(" None") &&
            !generatorType.equals("None", ignoreCase = true)
    }

    private fun LsiField.sequenceName(): String? {
        val strategy = annotationValue("GeneratedValue", "strategy")
        if (strategy?.contains("SEQUENCE", ignoreCase = true) != true) {
            return null
        }
        return annotationValue("GeneratedValue", "sequenceName")
            ?: annotationValue("GeneratedValue", "generatorName")
    }

    private fun LsiField.length(): Int? {
        return annotationValue("Length", "value")?.toIntOrNull()
            ?: annotationValue("Length", "max")?.toIntOrNull()
            ?: annotationValue("Size", "max")?.toIntOrNull()
            ?: annotationValue("Column", "length")?.toIntOrNull()
    }

    private fun LsiField.precision(): Int? {
        return annotationValue("Column", "precision")?.toIntOrNull()
            ?: annotationValue("Precision", "value")?.toIntOrNull()
    }

    private fun LsiField.scale(): Int? {
        return annotationValue("Column", "scale")?.toIntOrNull()
            ?: annotationValue("Scale", "value")?.toIntOrNull()
    }

    private fun LsiField.isTextType(): Boolean {
        return hasAnnotationSimple("Lob") ||
            annotationValue("Column", "sqlType")?.contains("TEXT", ignoreCase = true) == true ||
            annotationValue("Column", "sqlType")?.contains("CLOB", ignoreCase = true) == true ||
            annotationValue("Column", "columnDefinition")?.contains("TEXT", ignoreCase = true) == true ||
            annotationValue("Column", "columnDefinition")?.contains("CLOB", ignoreCase = true) == true
    }

    private fun LsiField.isJsonType(): Boolean {
        return isSerializedScalar() ||
            annotationValue("Column", "sqlType")?.contains("JSON", ignoreCase = true) == true ||
            annotationValue("Column", "columnDefinition")?.contains("JSON", ignoreCase = true) == true
    }

    private fun LsiField.isSerializedScalar(): Boolean {
        return hasAnnotationSimple("Serialized")
    }

    private fun LsiField.nativeTypeHint(): String? {
        return annotationValue("Column", "sqlType")?.takeIf { it.isNotBlank() }
            ?: annotationValue("Column", "columnDefinition")?.takeIf { it.isNotBlank() }
    }

    private fun LsiField.isUniqueField(): Boolean {
        return hasAnnotationSimple("Unique") ||
            annotation("Column")?.getAttribute("unique")?.toString()?.toBooleanStrictOrNull() == true
    }

    private fun LsiField.joinColumnName(): String? {
        return annotationValue("JoinColumn", "name")
    }

    private fun LsiField.referencedColumnName(): String? {
        return annotationValue("JoinColumn", "referencedColumnName")
    }

    private fun LsiField.hasAnnotationSimple(vararg simpleNames: String): Boolean {
        return annotations.any { annotation ->
            simpleNames.any { annotation.simpleName.equals(it, ignoreCase = true) }
        }
    }

    private fun LsiField.annotation(simpleName: String): LsiAnnotation? {
        return annotations.firstOrNull { it.simpleName.equals(simpleName, ignoreCase = true) }
    }

    private fun LsiClass.annotation(simpleName: String): LsiAnnotation? {
        return annotations.firstOrNull { it.simpleName.equals(simpleName, ignoreCase = true) }
    }


    /**
     * 从 @JoinTable 的 filter 属性中提取 JoinTableFilter 的 columnName，
     * 生成中间表的额外列（如 mapping_type）。
     * 使用反射处理嵌套注解，避免模块依赖 KSP。
     */
    private fun LsiField.readJoinTableFilterColumns(): List<AutoDdlColumn> {
        val joinTableAnno = annotation("JoinTable") ?: return emptyList()
        val filterValue = joinTableAnno.getAttribute("filter") ?: return emptyList()
        return extractFilterColumnNames(filterValue).map { columnName ->
            AutoDdlColumn(columnName, AutoDdlLogicalType.STRING, nullable = false, primaryKey = true)
        }.distinctBy { it.name.lowercase() }
    }

    /**
     * 从 JoinTableFilter 嵌套注解对象中提取 columnName。
     * filterValue 在 KSP 实现中是 KSAnnotation，通过反射读取其 arguments。
     */
    private fun extractFilterColumnNames(filterValue: Any): List<String> {
        return try {
            val argumentsMethod = filterValue::class.java.methods
                .firstOrNull { it.name == "getArguments" }
            @Suppress("UNCHECKED_CAST")
            val arguments = argumentsMethod?.invoke(filterValue) as? List<*> ?: return emptyList()
            arguments.mapNotNull { arg ->
                try {
                    val nameMethod = arg?.javaClass?.methods?.firstOrNull { it.name == "getName" }
                    val valueMethod = arg?.javaClass?.methods?.firstOrNull { it.name == "getValue" }
                    // KSName.asString() 返回 String
                    val nameObj = nameMethod?.invoke(arg)
                    val name = when (nameObj) {
                        is String -> nameObj
                        else -> nameObj?.javaClass?.methods
                            ?.firstOrNull { it.name == "asString" }
                            ?.invoke(nameObj)?.toString()
                    }
                    if (name == "columnName") {
                        valueMethod?.invoke(arg)?.toString()
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun LsiField.annotationValue(simpleName: String, attributeName: String): String? {
        return annotation(simpleName)?.getAttribute(attributeName)?.toString()
    }

    private val ENTITY_ANNOTATIONS = setOf(
        "org.babyfish.jimmer.sql.Entity",
        "jakarta.persistence.Entity",
        "javax.persistence.Entity",
    )
}
