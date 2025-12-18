plugins {
    id("site.addzero.gradle.plugin.kotlin-convention") version "+"
}

dependencies {
    implementation(project(":checkouts:lsi:lsi-core"))
    implementation("site.addzero:tool-str:2025.12.04")

}

description = "DDL Generator Core - 核心数据模型（无外部依赖）"
