package com.toggl.komposable.buildsrc

object Versions {
    const val ktlint = "0.38.1"
}

object Libs {
    const val androidGradlePlugin = "com.android.tools.build:gradle:7.1.2"
    const val gradleVersionsPlugin = "com.github.ben-manes:gradle-versions-plugin:0.42.0"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:2.9.1"
    // fixes warning thrown by SLF4J
    const val slf4j = "org.slf4j:slf4j-simple:1.7.26"

    object Test {
        const val kotestVersion = "4.2.3"

        const val junit5Plugin = "de.mannodermaus.gradle.plugins:android-junit5:1.7.1.1"
        const val mockk = "io.mockk:mockk:1.12.3"
        const val turbine = "app.cash.turbine:turbine:0.8.0"
        const val kotestMatchers = "io.kotest:kotest-assertions-core-jvm:${kotestVersion}"

        object Jupiter {
            private const val version = "5.7.1"
            // (Required) Writing and executing Unit Tests on the JUnit5 Platform
            const val api = "org.junit.jupiter:junit-jupiter-api:$version"
            const val engine = "org.junit.jupiter:junit-jupiter-engine:$version"

        }
    }

    object Google {
        const val material = "com.google.android.material:material:1.3.0-beta01"
    }

    object Kotlin {
        const val version = "1.6.10"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"

        object Test {
            const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:$version"
            const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:$version"
            const val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:$version"
        }
    }

    object Coroutines {
        private const val version = "1.6.1"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"

        object Test {
            const val kotlinCoroutineTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object AndroidX {

        object Activity {
            const val version = "1.4.0"
            const val activityKtx = "androidx.activity:activity-ktx:$version"
            const val activityCompose = "androidx.activity:activity-compose:$version"
        }

        const val appcompat = "androidx.appcompat:appcompat:1.4.0"
        const val coreKtx = "androidx.core:core-ktx:1.6.0"

        object Compose {
            const val version = "1.1.0"
            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val compiler = "androidx.compose.compiler:compiler:$version"
            const val animation = "androidx.compose.animation:animation:$version"


            object UI {
                const val core = "androidx.compose.ui:ui:$version"
                const val tooling = "androidx.compose.ui:ui-tooling:$version"
                const val viewbinding = "androidx.compose.ui:ui-viewbinding:$version"
                const val junit = "androidx.compose.ui:ui-test-junit4:$version"
                const val testManifest = "androidx.compose.ui:ui-test-manifest:$version"
            }

            object Material {
                const val core = "androidx.compose.material:material:$version"
                const val ripple = "androidx.compose.material:material-ripple"
                const val icons = "androidx.compose.material:material-icons-extended:$version"
            }

            object Foundation {
                const val layout = "androidx.compose.foundation:foundation-layout:$version"
                const val core = "androidx.compose.foundation:foundation:$version"
            }
        }

        object Test {
            private const val version = "1.3.0"
            const val core = "androidx.test:core:$version"
            const val runner = "androidx.test:runner:$version"
            const val rules = "androidx.test:rules:$version"
            const val junit = "androidx.test.ext:junit:1.1.1"
            const val espressoCore = "androidx.test.espresso:espresso-core:3.3.0"
        }

        object Lifecycle {
            private const val version = "2.4.0"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
            const val viewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:$version"
            const val ktx = "androidx.lifecycle:lifecycle-livedata-ktx:$version"
            const val commonJava8 = "androidx.lifecycle:lifecycle-common-java8:$version"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:$version"
            const val runtime = "androidx.lifecycle:lifecycle-runtime-ktx:$version"
        }

        object Navigation {
            private const val version = "2.4.0-alpha07"
            const val compose = "androidx.navigation:navigation-compose:$version"
        }

        object Hilt {
            private const val version = "1.0.0"
            const val compiler = "androidx.hilt:hilt-compiler:$version"
            const val navigation = "androidx.hilt:hilt-navigation-compose:1.0.0-alpha03"
            const val lifecycleViewModel = "androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03"
        }

        object Room {
            private const val version = "2.4.2"
            const val common = "androidx.room:room-common:$version"
            const val runtime = "androidx.room:room-runtime:$version"
            const val compiler = "androidx.room:room-compiler:$version"
            const val ktx = "androidx.room:room-ktx:$version"
        }
    }

    object Hilt {
        private const val version = "2.40.5"

        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val hilt = "com.google.dagger:hilt-android:$version"
        const val compiler = "com.google.dagger:hilt-android-compiler:$version"
    }
}

