package site.addzero.util.ddlgenerator.delta

import site.addzero.util.db.DatabaseType
import site.addzero.util.ddlgenerator.api.DdlGenerator
import site.addzero.util.lsi.clazz.LsiClass

/**
     * 生成差量 DDL
     */
     fun generateDeltaDdl(lsiClasses: List<LsiClass>) {
        if (lsiClasses.isEmpty()) return

    val fromUrl = DatabaseType.fromUrl("")
    // 获取 DDL 生成策略
        val strategy = DdlGenerator.getStrategy(DatabaseType.valueOf(settings.getDatabaseType()))

        // 创建默认的 SettingContext（离线模式使用）
        val defaultSettings = DatabaseConnectionSettings(
            jdbcUrl = "",
            jdbcUsername = "",
            jdbcPassword = ""
        )
        val deltaGenerator = DeltaDdlGenerator(defaultSettings, strategy)

        // 检查是否配置了数据库连接
        if (hasDatabaseConnection()) {
            // 在线模式：从数据库读取元数据
            generateOnlineDeltaDdl(lsiClasses, deltaGenerator)
        } else {
            // 离线模式：使用原有逻辑
            generateOfflineDeltaDdl(lsiClasses, deltaGenerator)
        }
    }
