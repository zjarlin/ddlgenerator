# ddlgenerator-lsi-adaptor

- 作用：把 `site.addzero.lsi.*` + Jimmer/JPA 注解语义转换为 `AutoDdlSchema`。
- Maven 坐标：`site.addzero:ddlgenerator-lsi-adaptor`
- 本地路径：`checkouts/ddlgenerator/ddlgenerator-lsi-adaptor`
- 约束：只做语义适配，不读取数据库、不生成 SQL。
- Jimmer 枚举：`@EnumType(NAME)` 或未声明策略时沿用文本类型回退，`@EnumType(ORDINAL)` 映射为 `INT32`。
- 字符串长度：读取 `@Length(value/max)`、`@Size(max)` 或支持长度属性的 `@Column(length)`；`@Lob` 和显式 `TEXT/CLOB` 仍映射为长文本类型。

```kotlin
val schema = LsiAutoDdlSchemaAdapter.from(lsiClasses)
```

关联约束直接使用已解析的物理列：默认外键遵循下划线命名，`@JoinColumn`/`@JoinColumns` 使用指定列名，复合外键按目标主键列排序。重复 `@Key(group = ...)` 和 `@Keys` 的每个分组均保留完整关联列，不会退化成仅含标量的唯一索引。含可空物理列的 Key 组继续整体跳过；`inputNotNull = true` 的关联列为非空。

正向 `@OneToOne` 始终为外键列生成唯一索引（可空时约束非空值），反向 `mappedBy` 不建列。普通 `@ManyToOne` 不隐式唯一；`ForeignKeyType.FAKE` 只关闭外键约束，不改变业务 Key。目标主键不存在、复合列缺失、引用非主键列或重复映射会明确报错。

`AssociationKeyDdlTest` 通过真实 Kotlin/KSP 验证上述关系、重复分组、复合主键，以及 PostgreSQL/MySQL/H2 的创建、修复和幂等输出。旧版错误索引的删除仍受 `allowDestructiveChanges` 控制，升级依赖不会绕过该设置。
