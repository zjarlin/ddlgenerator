import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
}

group = "site.addzero"
version = providers.gradleProperty("releaseVersion").get()
repositories { mavenCentral() }
kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.ENABLE)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
dependencies {
    if (project.name == "ddlgenerator-core") {
        api("site.addzero:tool-database-model:2026.08.26")
    } else {
        api(project(":ddlgenerator-core"))
    }
    if (project.name == "ddlgenerator-lsi-adaptor") {
        val lsiVersion = providers.gradleProperty("lsiVersion").get()
        api("site.addzero:lsi-core:$lsiVersion")
        testImplementation("site.addzero:lsi-ksp:$lsiVersion")
        testImplementation("com.google.devtools.ksp:symbol-processing-api:2.3.9")
        testImplementation("org.babyfish.jimmer:jimmer-core:0.11.2")
        testImplementation("dev.zacsweers.kctfork:ksp:0.7.1") {
            exclude(module = "symbol-processing-api")
        }
        testImplementation(project(":ddlgenerator-dialect-postgresql"))
        testImplementation(project(":ddlgenerator-dialect-mysql"))
        testImplementation(project(":ddlgenerator-dialect-h2"))
    }
    testImplementation(kotlin("test-junit"))
}
tasks.test {
    useJUnit()
    maxHeapSize = "2g"
}
mavenPublishing {
    configure(KotlinJvm(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    pom {
        name.set(project.name)
        description.set("Schema adaptation and SQL generation: ${project.name}")
        url.set("https://github.com/zjarlin/ddlgenerator")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("zjarlin")
                name.set("zjarlin")
                email.set("zjarlin@outlook.com")
            }
        }
        scm {
            connection.set("scm:git:https://github.com/zjarlin/ddlgenerator.git")
            url.set("https://github.com/zjarlin/ddlgenerator")
        }
    }
}
