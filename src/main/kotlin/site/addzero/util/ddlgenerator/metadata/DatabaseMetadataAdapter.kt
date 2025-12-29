package site.addzero.util.ddlgenerator.metadata

import org.babyfish.jimmer.config.autoddl.Settings
import org.koin.core.annotation.Single
import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.DatabaseMetadataReader


val databaseMetadataReader = DatabaseMetadataReader(
    url = Settings.jdbcUrl,
    username = Settings.jdbcUsername,
    password = Settings.jdbcPassword
)
fun testConnection(): List<JdbcTableMetadata> {
    val tableMetaData = databaseMetadataReader.getTableMetaData(excludeRules = Settings.autoddlExcludeTables)
    return tableMetaData
}