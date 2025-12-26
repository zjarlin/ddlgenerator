import org.babyfish.jimmer.config.autoddlConfigMap

plugins {
    id("site.addzero.gradle.plugin.kotlin-convention") version "+"
    id("site.addzero.gradle.plugin.processorbuddy.processor-buddy") version "2025.12.29"

    alias(libs.plugins.ksp)
}

processorBuddy {
    mustMap = autoddlConfigMap
    packageName = "org.babyfish.jimmer.config.autoddl"
//    readmeEnabled = true
//    settingContextEnabled=true
//    settingsObjectEnabled=false
}

//kotlin{
//    compilerOptions {
//        freeCompilerArgs.add("-Xcontext-parameters")
//    }
//}
//

dependencies {
    implementation(project(":checkouts:lsi:lsi-core"))
    implementation(project(":checkouts:lsi:lsi-database"))
    implementation("site.addzero:tool-database-model:2025.12.23")
    implementation("site.addzero:tool-jdbc:2025.12.24")
    implementation("site.addzero:tool-yml:2025.12.26")
    implementation("site.addzero:tool-str:2025.12.22")
    implementation("site.addzero:tool-sql-executor:2025.11.26")
    implementation("site.addzero:ioc-core-jvm:2025.12.28")
    ksp("site.addzero:ioc-processor-jvm:2025.12.28")

}
description = "ddl生成工具类"
