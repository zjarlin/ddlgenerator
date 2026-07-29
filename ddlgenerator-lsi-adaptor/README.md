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
