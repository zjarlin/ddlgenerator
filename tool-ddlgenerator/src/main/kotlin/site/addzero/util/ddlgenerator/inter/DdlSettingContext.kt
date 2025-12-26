package site.addzero.util.ddlgenerator.inter

import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerationStrategy
import site.addzero.util.ddlgenerator.api.getStrategy
import site.addzero.util.ddlgenerator.assist.extractSchemaFromUrl

@Deprecated("",replaceWith = ReplaceWith("org.babyfish.jimmer.config.autoddl" +
        ".SettingContext"))
interface DdlSettingContext {

    //允许删除表
     val allowDrop:Boolean





    val springResourcePath: String
    val jdbcUrl: String
    val jdbcUsername: String
    val jdbcPassword: String
    val databaseType: DatabaseType
        get() {
            val fromUrl = DatabaseType.fromUrl(jdbcUrl)
            return fromUrl ?: throw IllegalArgumentException("Unsupported database type")
        }
    val schema: String? get() = extractSchemaFromUrl(jdbcUrl)

    val autoddlExcludeTables: List<String>?

    val strategy: DdlGenerationStrategy
        get() {
            val strategy1 = getStrategy(databaseType)
            return strategy1
        }


}
