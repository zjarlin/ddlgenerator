pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
rootProject.name = "ddlgenerator-release"
listOf("ddlgenerator-core", "ddlgenerator-lsi-adaptor", "ddlgenerator-dialect-postgresql", "ddlgenerator-dialect-mysql", "ddlgenerator-dialect-h2").forEach { name ->
    include(":$name")
    project(":$name").projectDir = file("../$name")
    project(":$name").buildFileName = "../release/module.gradle.kts"
}
