package site.addzero.util.lsi.database.dialect


/**
 * 数据库类型映射接口
 *
 * 桥接 JDBC Types 到各数据库原生类型
 * 每种方言实现此接口，提供从 JDBC 类型代码到原生类型名称的转换
 */
interface DatabaseTypeMapping {
}
