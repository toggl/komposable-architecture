pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Komposable Architecture"

include(
    ":komposable-architecture",
    ":komposable-architecture-test",
    ":komposable-architecture-compiler",
    "samples:todos"
)
