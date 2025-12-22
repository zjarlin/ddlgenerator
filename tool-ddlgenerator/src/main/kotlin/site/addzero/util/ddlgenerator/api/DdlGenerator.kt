package site.addzero.util.ddlgenerator.api

import site.addzero.ioc.registry.getSupportStrategty
import site.addzero.util.db.DatabaseType

/**
 * DDL生成器工厂 - 使用SPI机制自动发现策略实现
 *
 * 通过 ServiceLoader 加载所有 DdlGenerationStrategy 实现，
 * 并根据数据库方言自动选择合适的策略
 *
 * 注意：通常不需要直接使用此工厂，推荐使用 LsiClass 和 LsiField 的扩展函数
 */
object DdlGenerator {

    /**
     * 使用ServiceLoader查找支持指定方言的策略
     *
     * @param dialect 数据库方言
     * @return 支持该方言的策略实现
     * @throws IllegalArgumentException 如果找不到支持该方言的策略
     */
    fun getStrategy(dialect: DatabaseType): DdlGenerationStrategy {
        val supportStrategty = getSupportStrategty<DdlGenerationStrategy> {
            it.supports(dialect)
        } ?: throw IllegalArgumentException(
            "not support yet: $dialect"
        )
        return supportStrategty
    }

}
