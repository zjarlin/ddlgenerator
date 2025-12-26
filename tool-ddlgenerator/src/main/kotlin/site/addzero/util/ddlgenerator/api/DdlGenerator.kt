package site.addzero.util.ddlgenerator.api

import site.addzero.ioc.registry.getSupportStrategty
import site.addzero.util.db.DatabaseType

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