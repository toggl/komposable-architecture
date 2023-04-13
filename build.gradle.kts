import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.diffplug.gradle.spotless.SpotlessExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.hilt.gradlePlugin)
        classpath(libs.junit5.gradlePlugin)
    }
}

plugins {
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

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            ktlint(libs.versions.ktlint.get())
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = JavaVersion.VERSION_11.toString()
            freeCompilerArgs += listOf(
                "-Xskip-prerelease-check",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview"
            )
        }
    }
}
