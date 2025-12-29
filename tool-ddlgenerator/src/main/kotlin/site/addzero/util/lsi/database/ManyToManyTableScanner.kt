package site.addzero.util.lsi.database

import site.addzero.util.lsi.anno.LsiAnnotation
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.database.model.ManyToManyTable
import site.addzero.util.lsi.field.LsiField

/**
 * 多对多关系扫描器
 * 负责扫描所有实体类，发现多对多关系并生成中间表信息
 */
object ManyToManyTableScanner {

    private val MANY_TO_MANY_ANNOTATIONS = setOf(
        "org.babyfish.jimmer.sql.ManyToMany"
    )

    /**
     * 扫描所有类，提取多对多关系中间表信息
     * 
     * @param classes 所有实体类列表
     * @return 中间表信息列表（去重后）
     */
    fun scanManyToManyTables(classes: List<LsiClass>): List<ManyToManyTable> {
        val tables = mutableMapOf<String, ManyToManyTable>()
        classes.forEach { lsiClass ->
            lsiClass.fields.forEach { field ->
                // 检查是否有ManyToMany注解
                val manyToManyAnno = field.annotations.firstOrNull {
                    it.qualifiedName in MANY_TO_MANY_ANNOTATIONS
                } ?: return@forEach
                // 检查是否是mappedBy侧（如果是，跳过，因为会在拥有侧处理）
                val mappedBy = manyToManyAnno.getAttribute("mappedBy")?.toString()
                if (!mappedBy.isNullOrBlank()) {
                    // 这是被维护侧，跳过
                    return@forEach
                }
                // 获取目标实体类型
                val targetEntityName = getTargetEntityName(field, manyToManyAnno)
                    ?: return@forEach
                // 查找目标实体类
                val targetEntity = classes.find {
                    it.qualifiedName == targetEntityName || it.name == targetEntityName
                } ?: return@forEach
                // 生成中间表信息
                val table = createManyToManyTable(lsiClass, field, targetEntity, manyToManyAnno)
                // 使用表名作为key去重
                if (!tables.containsKey(table.tableName)) {
                    tables[table.tableName] = table
                }
            }
        }
        return tables.values.toList()
    }

    /**
     * 获取目标实体类型名
     */
    private fun getTargetEntityName(field: LsiField, annotation: LsiAnnotation): String? {
        // 1. 从注解的targetEntity属性获取
        annotation.getAttribute("targetEntity")?.toString()?.let {
            if (it != "void" && it.isNotBlank()) {
                return it
            }
        }
        // 2. 从字段类型的泛型参数获取
        val fieldType = field.type
        if (fieldType?.isCollectionType == true) {
            val typeParams = fieldType.typeParameters
            if (typeParams.isNotEmpty()) {
                //一般是第一个泛型参数
                val qualifiedName = typeParams[0].qualifiedName
                return qualifiedName
            }
        }
        return null
    }

    /**
     * 创建多对多中间表信息
     */
    private fun createManyToManyTable(
        leftEntity: LsiClass,
        field: LsiField,
        rightEntity: LsiClass,
        annotation: LsiAnnotation
    ): ManyToManyTable {
        val leftTableName = leftEntity.guessTableName
        val rightTableName = rightEntity.guessTableName

        // 生成中间表名
        val tableName = getJoinTableName(annotation)
            ?: ManyToManyTable.generateTableName(leftTableName, rightTableName)

        // 生成列名
        val leftColumnName = getJoinColumnName(annotation, "joinColumns")
            ?: "${leftTableName}_id"
        val rightColumnName = getJoinColumnName(annotation, "inverseJoinColumns")
            ?: "${rightTableName}_id"

        val manyToManyTable = ManyToManyTable(
            tableName = tableName,
            leftTableName = leftTableName,
            leftColumnName = leftColumnName,
            rightTableName = rightTableName,
            rightColumnName = rightColumnName,
            leftEntity = leftEntity,
            rightEntity = rightEntity,
            field = field
        )
        return manyToManyTable
    }

    /**
     * 从@JoinTable注解获取中间表名
     */
    private fun getJoinTableName(manyToManyAnno: LsiAnnotation): String? {
        // todo 查找关联的@JoinTable注解
        // 这里简化处理，直接从ManyToMany注解中查找
        return null  // 如果有@JoinTable注解，可以在这里解析
    }

    /**
     * 获取连接列名
     */
    private fun getJoinColumnName(
        manyToManyAnno: LsiAnnotation,
        attributeName: String
    ): String? {
        // todo 从@JoinTable注解的joinColumns或inverseJoinColumns获取
        return null  // 简化处理，使用默认命名
    }
}

/**
 * 扩展函数：扫描多对多中间表
 */
fun List<LsiClass>.scanManyToManyTables(): List<ManyToManyTable> {
    return ManyToManyTableScanner.scanManyToManyTables(this)
}
