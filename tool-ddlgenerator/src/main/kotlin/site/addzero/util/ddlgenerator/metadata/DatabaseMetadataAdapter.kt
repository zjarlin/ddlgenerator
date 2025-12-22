package site.addzero.util.ddlgenerator.metadata

import org.babyfish.jimmer.apt.config.Settings
import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.DatabaseMetadataReader

/**
 * 数据库元数据提取器适配器
 * 将 DatabaseMetadataUtil 包装，方便增量 DDL 生成器使用
 */
class DatabaseMetadataAdapter(
    private val settings: Settings
) {
    /**
     * 提取数据库元数据
     * @param includeTables 包含的表规则（支持通配符 *）
     * @param excludeTables 排除的表规则（支持通配符 *）
     * @param schema 数据库 schema
     * @return 表元数据列表
     */
    fun extractDatabaseMetadata(
        includeTables: List<String>? = null,
        excludeTables: List<String>? = null,
        schema: String? = null
    ): List<JdbcTableMetadata> {
        val databaseMetadataReader = DatabaseMetadataReader(settings.jdbcUrl, settings.jdbcUsername, settings.jdbcPassword)

        databaseMetadataReader.getTableMetaData(
            schema = "public",
            includeRules = null,
            excludeRules = null
        )
        // 建立数据库连接
        val connection = createConnection()

        try {
            // 使用 DatabaseMetadataUtil 提取元数据
            val tables = DatabaseMetadataUtil.getTableMetaData(
                connection = connection,
                schema = schema ?: "public",
                includeRules = includeTables,
                excludeRules = excludeTables
            )

            return tables
        } finally {
            connection.close()
        }
    }

}