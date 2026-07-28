plugins {
    id("site.addzero.buildlogic.jvm.kotlin-convention")
}

val libs = versionCatalogs.named("libs")
val ddlGeneratorRootPath = project.path.substringBeforeLast(":")

dependencies {
    api(project("$ddlGeneratorRootPath:ddlgenerator-core"))
    api(project(":lib:lsi:lsi-core"))
}

description = "AutoDDL 的 LSI 输入适配层"
