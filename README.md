# DDL Generator

基于统一 Schema、差异规划和数据库方言的 AutoDDL 生成器，当前权威实现来自 OKM Platform。

包含模块：

- `ddlgenerator-core`
- `ddlgenerator-dialect-postgresql`
- `ddlgenerator-dialect-mysql`
- `ddlgenerator-dialect-h2`
- `ddlgenerator-lsi-adaptor`

仓库作为 Gradle 多模块工程的 Git submodule 引入，各模块发布坐标保持 `site.addzero:ddlgenerator-*`。
