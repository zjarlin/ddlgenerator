# ddlgenerator-core

- 作用：提供 AutoDDL 纯领域模型、schema diff planner、DDL operation 模型、方言 SPI。
- Maven 坐标：`site.addzero:ddlgenerator-core`
- 本地路径：`checkouts/ddlgenerator/ddlgenerator-core`
- 约束：不依赖 LSI、JDBC reader、YAML、Settings 或 Koin。

```kotlin
val operations = SchemaDiffPlanner.plan(desiredSchema, actualSchema, AutoDdlDiffOptions())
val sql = AutoDdlDialects.require(DatabaseType.POSTGRESQL).render(operations)
```

Flyway 输出可通过 `FlywayMigrationVersionGenerator` 结合数据库已执行版本、源码现有迁移、模块命名空间和 SQL 内容生成稳定版本。生成器不会复用已经落后于数据库当前版本的同内容迁移，并保证新版本严格大于所有已知版本。
