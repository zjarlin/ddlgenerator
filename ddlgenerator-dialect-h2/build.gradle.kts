plugins {
    id("site.addzero.buildlogic.jvm.kotlin-convention")
}

val ddlGeneratorRootPath = project.path.substringBeforeLast(":")

dependencies {
    api(project("$ddlGeneratorRootPath:ddlgenerator-core"))
}

description = "AutoDDL H2 方言"
