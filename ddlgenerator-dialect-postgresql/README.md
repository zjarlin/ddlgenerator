# ddlgenerator-dialect-postgresql

- 作用：提供 PostgreSQL 的 AutoDDL 方言实现。
- Maven 坐标：`site.addzero:ddlgenerator-dialect-postgresql`
- 本地路径：`checkouts/ddlgenerator/ddlgenerator-dialect-postgresql`
- 约束：通过 `ServiceLoader` 注册到 runtime。
- 字符串：没有显式长度时生成 `TEXT`；存在 `@Length`、`@Size` 等显式长度时生成 `VARCHAR(n)`。
- 差异计算：在快照和数据库比较前把无长度 `STRING` 归一化为物理 `TEXT`，方言默认类型变化也会生成迁移。
