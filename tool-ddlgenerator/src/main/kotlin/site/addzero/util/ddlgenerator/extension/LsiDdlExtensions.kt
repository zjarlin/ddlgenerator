package site.addzero.util.ddlgenerator.extension

import org.babyfish.jimmer.config.autoddl.Settings
import site.addzero.util.ddlgenerator.config.strategy
import site.addzero.util.lsi.clazz.LsiClass
import site.addzero.util.lsi.clazz.guessTableName
import site.addzero.util.lsi.field.LsiField

private inline val currentStrategy get() = Settings.strategy

/**
 * 生成CREATE TABLE的DDL语句
 */
fun LsiClass.toCreateTableDDL(): String = 
    currentStrategy.generateCreateTable(this)

/**
 * 生成DROP TABLE的DDL语句
 */
fun LsiClass.toDropTableDDL(): String = 
    currentStrategy.generateDropTable(this.guessTableName)

/**
 * 生成ALTER TABLE添加注释的DDL语句
 */
fun LsiClass.toAddCommentDDL(): String = 
    currentStrategy.generateAddComment(this)

// ============ LsiField 扩展函数 ============
/**
 * 生成ADD COLUMN的DDL语句
 */
fun LsiField.toAddColumnDDL(tableName: String): String = 
    currentStrategy.generateAddColumn(tableName, this)

/**
 * 生成DROP COLUMN的DDL语句
 */
fun LsiField.toDropColumnDDL(tableName: String): String = 
    currentStrategy.generateDropColumn(
        tableName, 
        this.columnName ?: this.name ?: "unknown"
    )

/**
 * 生成MODIFY COLUMN的DDL语句
 */
fun LsiField.toModifyColumnDDL(tableName: String): String = 
    currentStrategy.generateModifyColumn(tableName, this)


