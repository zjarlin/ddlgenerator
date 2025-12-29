package site.addzero.util.ddlgenerator.metadata

import org.babyfish.jimmer.config.autoddl.SettingContext
import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.DatabaseMetadataReader
import site.addzero.util.db.DatabaseType

fun extractDatabaseMetadata(ddlSettingContext: SettingContext): List<JdbcTableMetadata> {
    val databaseType = DatabaseType.fromUrl(ddlSettingContext.jdbcUrl)
    val databaseMetadataReader = DatabaseMetadataReader(
        ddlSettingContext.jdbcUrl,
        ddlSettingContext.jdbcUsername,
        ddlSettingContext.jdbcPassword
    )
    val excludeRules = ddlSettingContext.autoddlExcludeTables
    val tableMetaData = databaseMetadataReader.getTableMetaData(
        excludeRules = excludeRules
    )
    return tableMetaData
}

