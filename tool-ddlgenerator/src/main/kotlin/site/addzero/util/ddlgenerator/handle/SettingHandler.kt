package site.addzero.util.ddlgenerator.handle

import site.addzero.util.DatabaseConfigReader

/**
 * Utility for populating JDBC related settings from Spring configuration.
 */
object SettingHandler {


    /**从springyml猜测配置
     * @param [settings]
     * @param [springResourcePathGetter]
     * @param [jdbcUrlGetter]
     * @param [jdbcUrlSetter]
     * @param [jdbcUsernameGetter]
     * @param [jdbcUsernameSetter]
     * @param [jdbcPasswordGetter]
     * @param [jdbcPasswordSetter]
     */
    fun <S> guessSettingsFromSpringYml(
        settings: S,
        springResourcePathGetter: (S) -> String?,
        jdbcUrlGetter: (S) -> String?,
        jdbcUrlSetter: (S, String?) -> Unit,
        jdbcUsernameGetter: (S) -> String?,
        jdbcUsernameSetter: (S, String?) -> Unit,
        jdbcPasswordGetter: (S) -> String?,
        jdbcPasswordSetter: (S, String?) -> Unit
    ) {
        val databaseConfigSettings = DatabaseConfigReader.fromSpringYml(springResourcePathGetter(settings))
        applyIfBlank(settings, jdbcUrlGetter, jdbcUrlSetter) { databaseConfigSettings?.jdbcUrl }
        applyIfBlank(settings, jdbcUsernameGetter, jdbcUsernameSetter) { databaseConfigSettings?.jdbcUsername }
        applyIfBlank(settings, jdbcPasswordGetter, jdbcPasswordSetter) { databaseConfigSettings?.jdbcPassword }
    }

    private fun <S> applyIfBlank(
        settings: S,
        getter: (S) -> String?,
        setter: (S, String?) -> Unit,
        fallbackSupplier: () -> String?
    ) {
        val currentValue = getter(settings)
        if (currentValue.isNullOrBlank()) {
            setter(settings, fallbackSupplier())
        }
    }
}
