rootProject.name = "Komposable Architecture"

include(
    ":komposable-architecture",
    ":komposable-architecture-test",
    ":todo-sample"
)

// Enable Gradle's version catalog support
// https://docs.gradle.org/current/userguide/platforms.html
enableFeaturePreview("VERSION_CATALOGS")