description = "Spigot / Paper plugin: commands, GUI reader, sounds, temporary books."

plugins {
    alias(libs.plugins.shadow)
}

// Compile against the oldest supported API (1.21) so the binary runs on every later server.
// Verify against the newest with: ./gradlew build -PspigotApi=26.3-R0.1-SNAPSHOT
val spigotApiVersion: String = providers.gradleProperty("spigotApi").getOrElse("1.21-R0.1-SNAPSHOT")

dependencies {
    implementation(project(":bibleplugin-core"))
    implementation(project(":bibleplugin-api"))
    implementation(project(":bibleplugin-source"))
    implementation(project(":bibleplugin-persistence"))
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")
}

tasks.processResources {
    // Local values only: capturing script-level objects would break the configuration cache.
    val tokens = mapOf(
        "version" to project.version.toString(),
        "apiVersion" to "1.21",
    )
    inputs.properties(tokens)
    filesMatching("plugin.yml") {
        expand(tokens)
    }
}

tasks.shadowJar {
    archiveBaseName = "BiblePlugin"
    archiveClassifier = ""
    mergeServiceFiles()
    relocate("com.google.gson", "org.bibleplugin.libs.gson")
}

// Only the shaded, relocated jar is a deliverable; the plain jar would miss the bundled dependencies.
tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
