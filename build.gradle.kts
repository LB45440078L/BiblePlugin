// Shared build logic for every BiblePlugin module.
// Requires a JDK 21+ to run Gradle; code is compiled with `--release 21` so the
// resulting classes run on every server from Minecraft 1.21 up to 26.x.

val javaRelease = 21

subprojects {
    apply(plugin = "java-library")

    group = rootProject.group
    version = rootProject.version

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(javaRelease)
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-parameters"))
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            encoding = "UTF-8"
            addStringOption("Xdoclint:-missing", "-quiet")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
        }
    }

    dependencies {
        "testImplementation"(platform(rootProject.libs.junit.bom))
        "testImplementation"(rootProject.libs.junit.jupiter)
        "testRuntimeOnly"(rootProject.libs.junit.launcher)
    }
}
