package site.addzero.util.ddlgenerator.metadata

import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.DatabaseMetadataReader
import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.inter.DdlSettingContext

fun extractDatabaseMetadata(ddlSettingContext: DdlSettingContext): List<JdbcTableMetadata> {
    val databaseType = DatabaseType.fromUrl(ddlSettingContext.jdbcUrl)
    val databaseMetadataReader = DatabaseMetadataReader(
        ddlSettingContext.jdbcUrl,
        ddlSettingContext.jdbcUsername,
        ddlSettingContext.jdbcPassword
    )
    val tableMetaData = databaseMetadataReader.getTableMetaData(
        excludeRules = ddlSettingContext.autoddlExcludeTables
    )
    return tableMetaData
}

