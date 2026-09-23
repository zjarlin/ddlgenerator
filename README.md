# DDL Generator

基于统一 Schema、差异规划和数据库方言的 DDL 生成器。

包含模块：

- `ddlgenerator-core`
- `ddlgenerator-dialect-postgresql`
- `ddlgenerator-dialect-mysql`
- `ddlgenerator-dialect-h2`
- `ddlgenerator-lsi-adaptor`

仓库作为 Gradle 多模块工程的 Git submodule 引入，各模块发布坐标保持 `site.addzero:ddlgenerator-*`。

## 计算属性

LSI 适配层根据 `LsiField.isComputed` 排除无存储字段的普通 getter，无需标注 `@Formula`。简单表达式、复杂代码块、继承属性和嵌入对象中的计算属性均不生成 SQL 列。已有的 `@Formula`、`@Transient` 等排除规则继续适用；抽象属性和有 backing field 的属性仍参与建表。

```kotlin
val schema = LsiAutoDdlSchemaAdapter.from(classes)
val operations = SchemaDiffPlanner.plan(schema, previousSchema)
```

调用方冻结 LSI 元数据时必须保留 `isComputed`。已有数据库中的错误列遵循现有的破坏性变更策略；本修复不修改已应用的 Flyway 文件。

## 独立验证与发布

使用 JDK 17、Gradle 9.1，在仓库根执行：

```sh
gradle -p release test publishToMavenLocal
gradle -p release tasks --all
gradle -p release publishToMavenCentral
```

`release/` 直接构建当前仓库源码，LSI 从 Maven Central 按 `release/gradle.properties` 中的 `lsiVersion` 获取，不使用宿主副本或 Maven Local。发布版本由 `releaseVersion` 指定，采用未占用的日期版本。签名及 Central 凭据通过用户 Gradle 属性或环境提供。
