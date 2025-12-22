package site.addzero.util.ddlgenerator.jdbcmetadata

import site.addzero.util.DatabaseMetadataUtil.getTableMetaData


class JdbcmetadataHelper(
   private val elements: Elements,
   private val logger: EnhancedAptLsiLogger,
   private val options: DdlGenerationOptions,
   private val ddlGenerator: DdlGenerator
){



    val tables = getTableMetaData(connection, config.jdbcSchema, config.includeTables, config.excludeTables)
}