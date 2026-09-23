# 模块维护

根 README 为使用入口。模块依赖 `ddlgenerator-core` 和 `site.addzero:lsi-core`；公共入口 `LsiAutoDdlSchemaAdapter.from(classes)` 生成结构化 schema，`scanManyToManyTables` 处理关联表。输入只接受 LSI，不在生产适配器中使用 KSP 或 APT 符号。

`LsiField.isComputed` 与静态属性、Formula、Transient 等非存储语义一起过滤。不要根据 getter 复杂度或属性名猜测，也不要误过滤抽象实体属性或存储字段。外部快照必须完整保存该事实。适配器无进程状态、数据库连接和资源关闭职责，异常直接交给调用方处理。

在仓库根使用 JDK 17、Gradle 9.1 执行 `gradle -p release test`。真实 KSP 回归测试覆盖简单/复杂/继承/嵌入 getter，并检查 PostgreSQL、MySQL、H2 建表、只添加计算属性时无迁移、增量只添加存储列及幂等性。发布入口在 `release/`，宿主入口在本模块 `build.gradle.kts`，依赖调整需保持二者一致。
