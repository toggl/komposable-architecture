import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.junit5) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.nexus)
}

extra.apply {
    set("PUBLISH_DESCRIPTION", "Kotlin implementation of Point-Free\'s composable architecture")
    set("PUBLISH_URL", "https://github.com/toggl/komposable-architecture")
    set("PUBLISH_LICENSE_NAME", "Apache License")
    set("PUBLISH_LICENSE_URL", "https://github.com/toggl/komposable-architecture/blob/main/LICENSE")
    set("PUBLISH_DEVELOPER_ID", "toggl")
    set("PUBLISH_DEVELOPER_NAME", "Toggl Track Android Team ♥️")
    set("PUBLISH_DEVELOPER_EMAIL", "support@toggl.com")
    set("PUBLISH_SCM_CONNECTION", "scm:git:github.com/toggl/komposable-architecture.git")
    set("PUBLISH_SCM_DEVELOPER_CONNECTION", "scm:git:ssh://github.com/toggl/komposable-architecture.git")
    set("PUBLISH_SCM_URL", "https://github.com/toggl/komposable-architecture/tree/main")
}

apply(from = "${rootDir}/scripts/publish-root.gradle")

subprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint(libs.versions.ktlint.get())
        }
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            allWarningsAsErrors = true
            jvmTarget = JvmTarget.JVM_11
        }
    }
}
