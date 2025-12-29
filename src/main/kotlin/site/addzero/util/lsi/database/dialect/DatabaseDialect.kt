package site.addzero.util.lsi.database.dialect

import site.addzero.util.db.DatabaseType

/**
 * 数据库方言接口
 *
 * 定义各数据库的 DDL 语法差异和类型映射
 */
interface DatabaseDialect {
    /**
     * 方言名称
     * 例如: "MySQL", "PostgreSQL", "Oracle", "SQL Server"
     */
    val databaseType: DatabaseType
}
