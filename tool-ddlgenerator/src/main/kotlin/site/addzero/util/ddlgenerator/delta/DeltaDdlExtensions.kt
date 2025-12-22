package site.addzero.util.ddlgenerator.delta

import jdk.nashorn.internal.runtime.PropertyMap.diff
import org.babyfish.jimmer.apt.config.SettingContext
import org.babyfish.jimmer.apt.config.Settings
import site.addzero.entity.JdbcTableMetadata
import site.addzero.util.DatabaseMetadataReader
import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerator
import site.addzero.util.ddlgenerator.diff.comparator.DefaultTableComparator
import site.addzero.util.ddlgenerator.diff.model.SchemaDiff
import site.addzero.util.lsi.clazz.LsiClass




/**
 * 生成差量 DDL（便捷方法）
 */
fun List<LsiClass>.generateDeltaDdl(
    settings: Settings,
): String {

    val url = settings.jdbcUrl

    val databaseType = DatabaseType.fromUrl(url)
    val databaseMetadataReader = DatabaseMetadataReader(
        url, settings
            .jdbcUsername, settings.jdbcPassword
    )
    databaseMetadataReader.getTableMetaData()


    val comparator = DefaultTableComparator()


    val strategy1 = DdlGenerator.getStrategy(databaseType)

    // 创建默认的 SettingContext
    val settingContext = object : SettingContext {
        override val jdbcUrl = ""
        override val jdbcUsername = ""
        override val jdbcPassword = ""
        // databaseType 会从 URL 推断，这里不需要设置
    }
    val generator = DeltaDdlGenerator(settingContext, strategy)

    return generator.generateDeltaDdl(diff)
}

/**
 * 对比差异（不生成 SQL）
 */
fun List<LsiClass>.compareTo(
    dbTables: List<JdbcTableMetadata>,
    config: Settings = Settings.DEFAULT
): SchemaDiff {
    val comparator = DefaultTableComparator()
    return comparator.compareSchema(this, dbTables, config)
}

/**
 * 生成差量 DDL（从差异对象）
 */
fun SchemaDiff.toDeltaDdl(databaseType: DatabaseType = DatabaseType.MYSQL): String {
    val strategy = DdlGenerator.getStrategy(databaseType)

    // 创建默认的 SettingContext
    val settingContext = object : SettingContext {
        override val jdbcUrl = ""
        override val jdbcUsername = ""
        override val jdbcPassword = ""
    }
    val generator = DeltaDdlGenerator(settingContext, strategy)
    return generator.generateDeltaDdl(this)
}
