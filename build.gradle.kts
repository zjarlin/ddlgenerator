//import org.babyfish.jimmer.config.autoddlConfigMap

plugins {
    id("site.addzero.gradle.plugin.kotlin-convention") version "+"
    id("site.addzero.gradle.plugin.processorbuddy.processor-buddy") version "2025.12.31.1327"
//    id("site.addzero.gradle.plugin.koin-convention") version "2025.12.30"
}
//processorBuddy {
//    mustMap = autoddlConfigMap
//    packageName = "org.babyfish.jimmer.config.autoddl"
//    readmeEnabled = true
////    settingContextEnabled=true
////    settingsObjectEnabled=false
//}
//kotlin{
//    compilerOptions {
//        freeCompilerArgs.add("-Xcontext-parameters")
//    }
//}
//
dependencies {
//    implementation(project(":checkouts:lsi:lsi-core"))
//    implementation(project(":project:jimmer-core"))
    implementation("site.addzero:tool-database-model:2025.12.23")
    implementation("site.addzero:tool-jdbc:2025.12.24")
    implementation("site.addzero:tool-yml:2025.12.26")
    implementation("site.addzero:tool-str:2025.12.30")
    implementation("site.addzero:tool-sql-executor:2025.11.26")


}
description = "ddl生成工具类"
