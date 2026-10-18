rootProject.name = "bibleplugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
            name = "spigotmc-snapshots"
        }
        maven("https://repo.md-5.net/content/repositories/public/") {
            name = "md-5"
        }
    }
}

include(
    "bibleplugin-core",
    "bibleplugin-source",
    "bibleplugin-persistence",
    "bibleplugin-api",
    "bibleplugin-spigot",
)
