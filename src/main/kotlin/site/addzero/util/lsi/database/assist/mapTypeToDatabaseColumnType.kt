package site.addzero.util.lsi.database.assist

import site.addzero.util.lsi.assist.TypeChecker
import java.sql.JDBCType

/**
 * 类型映射逻辑
 * 将 Java/Kotlin 类型映射到数据库列类型键
 *
 * 注意：必须先检查Long再检查Int，因为TypeChecker.isIntType()会匹配所有INTEGER category的类型（包括Long）
 *
 * @return DatabaseType 的属性名（遵循 java.sql.Types 定义），例如 "BIGINT", "INTEGER", "VARCHAR" 等
 */
fun mapTypeToDatabaseColumnType(typeName: String): String {
    return when {
        TypeChecker.isLongType(typeName) -> JDBCType.BIGINT.name
        TypeChecker.isIntType(typeName) -> JDBCType.INTEGER.name
        TypeChecker.isBigDecimalType(typeName) ->JDBCType.DECIMAL.name
        TypeChecker.isFloatType(typeName) -> JDBCType.FLOAT.name
        TypeChecker.isDoubleType(typeName) -> JDBCType.DOUBLE.name
        TypeChecker.isStringType(typeName) -> JDBCType.VARCHAR.name
        TypeChecker.isDateType(typeName) -> JDBCType.DATE.name
        TypeChecker.isDateTimeType(typeName) -> JDBCType.TIMESTAMP.name
        TypeChecker.isBooleanType(typeName) -> JDBCType.BOOLEAN.name
        TypeChecker.isCharType(typeName) -> JDBCType.CHAR.name
        else -> JDBCType.VARCHAR.name
    }
}
