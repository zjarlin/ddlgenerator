# Processor Configuration

## Consumer Usage

### Gradle (KSP)

\`\`\`kotlin
plugins {
    id("com.google.devtools.ksp")
}

ksp {
    arg("springResourcePath", "")
    arg("jdbcUrl", "")
    arg("jdbcUsername", "")
    arg("jdbcPassword", "")
    arg("jdbcPassword1", "true")
    arg("jdbcPassword2", "1")
    arg("jdbcPassword3", "1.1")
    arg("jdbcPassword4", "false")
    arg("autoddlExcludeTables", "1,2,3,333")
}
\`\`\`

---

### Maven (APT)

\`\`\`xml
<properties>
    <apt.springResourcePath></apt.springResourcePath>
    <apt.jdbcUrl></apt.jdbcUrl>
    <apt.jdbcUsername></apt.jdbcUsername>
    <apt.jdbcPassword></apt.jdbcPassword>
    <apt.jdbcPassword1>true</apt.jdbcPassword1>
    <apt.jdbcPassword2>1</apt.jdbcPassword2>
    <apt.jdbcPassword3>1.1</apt.jdbcPassword3>
    <apt.jdbcPassword4>false</apt.jdbcPassword4>
    <apt.autoddlExcludeTables>1,2,3,333</apt.autoddlExcludeTables>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <compilerArgs>
                    <arg>-AspringResourcePath=${apt.springResourcePath}</arg>
                    <arg>-AjdbcUrl=${apt.jdbcUrl}</arg>
                    <arg>-AjdbcUsername=${apt.jdbcUsername}</arg>
                    <arg>-AjdbcPassword=${apt.jdbcPassword}</arg>
                    <arg>-AjdbcPassword1=${apt.jdbcPassword1}</arg>
                    <arg>-AjdbcPassword2=${apt.jdbcPassword2}</arg>
                    <arg>-AjdbcPassword3=${apt.jdbcPassword3}</arg>
                    <arg>-AjdbcPassword4=${apt.jdbcPassword4}</arg>
                    <arg>-AautoddlExcludeTables=${apt.autoddlExcludeTables}</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
\`\`\`

---

## Library Author Usage

Apply the plugin in your `build.gradle.kts`:

\`\`\`kotlin
plugins {
    id("site.addzero.gradle.plugin.processorbuddy")
}

processorBuddy {
    mustMap.set(mapOf(
        "springResourcePath" to "",
        "jdbcUrl" to "",
        "jdbcUsername" to "",
        "jdbcPassword" to "",
        "jdbcPassword1" to "true",
        "jdbcPassword2" to "1",
        "jdbcPassword3" to "1.1",
        "jdbcPassword4" to "false",
        "autoddlExcludeTables" to "1,2,3,333"
    ))
}
\`\`\`

This generates:
- Interface: `org.babyfish.jimmer.config.autoddl.SettingContext`
- Singleton: `org.babyfish.jimmer.config.autoddl.Settings`

\`\`\`kotlin
import org.babyfish.jimmer.config.autoddl.SettingContext

// Get singleton
val config = SettingContext.instance()

// Load config
config.fromOptions(mapOf("key" to "value"))

// Export config
val options = config.toOptions()

// Merge configs (non-empty values win)
val merged = SettingContext.merge(config1, config2)
\`\`\`