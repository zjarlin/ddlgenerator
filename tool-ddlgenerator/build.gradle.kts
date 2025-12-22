plugins {
    id("site.addzero.gradle.plugin.kotlin-convention") version "+"
    alias(libs.plugins.ksp)
    id("apt4autoddl")
}
dependencies {
    implementation(project(":checkouts:lsi:lsi-core"))
    implementation(project(":checkouts:lsi:lsi-database"))
    implementation("site.addzero:tool-database-model:2025.12.23")
    implementation("site.addzero:tool-jdbc:2025.12.24")
    implementation("site.addzero:tool-yml:2025.12.24")
    implementation("site.addzero:tool-str:2025.12.22")
    implementation("site.addzero:tool-sql-executor:2025.11.26")
    implementation("site.addzero:ioc-core-jvm:2025.12.24")
    ksp("site.addzero:ioc-processor-jvm:2025.12.23")

}
