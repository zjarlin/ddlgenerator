plugins {
    id("site.addzero.buildlogic.jvm.kotlin-convention")
}

val libs = versionCatalogs.named("libs")
val ddlGeneratorRootPath = project.path.substringBeforeLast(":")

dependencies {
    testImplementation("com.h2database:h2:2.4.240")
    api(project("$ddlGeneratorRootPath:ddlgenerator-core"))
    api("site.addzero:lsi-core:2026.09.24")
    testImplementation(kotlin("test-junit"))
    testImplementation("site.addzero:lsi-ksp:2026.09.24")
    testImplementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
    testImplementation("org.babyfish.jimmer:jimmer-core:0.11.2")
    testImplementation("dev.zacsweers.kctfork:ksp:0.7.1") {
        exclude(module = "symbol-processing-api")
    }
    testImplementation(project("$ddlGeneratorRootPath:ddlgenerator-dialect-postgresql"))
    testImplementation(project("$ddlGeneratorRootPath:ddlgenerator-dialect-mysql"))
    testImplementation(project("$ddlGeneratorRootPath:ddlgenerator-dialect-h2"))
}

description = "AutoDDL 的 LSI 输入适配层"

tasks.test {
    useJUnit()
    maxHeapSize = "4g"
}
