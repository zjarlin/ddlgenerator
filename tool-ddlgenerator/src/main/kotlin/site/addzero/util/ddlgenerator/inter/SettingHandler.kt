package site.addzero.util.ddlgenerator.inter

import org.babyfish.jimmer.apt.config.Settings
import site.addzero.util.SpringYmlUtil

class SettingHandler(val settings: Settings) {
    fun guessSettings() {
        val springYmlUtil = SpringYmlUtil(settings.springResourcePath)

        val guessJdbcUrl = springYmlUtil.getActivateYmlPropertiesString("spring.datasource.url")
        val guessUserName = springYmlUtil.getActivateYmlPropertiesString("spring.datasource.username")
        val guessPassword = springYmlUtil.getActivateYmlPropertiesString("spring.datasource.password")


        val jdbcUrl = settings.jdbcUrl?:guessJdbcUrl()
    }
}