plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("de.mannodermaus.android-junit5")
}

kapt {
    correctErrorTypes = true
    useBuildCache = true
}

android {
    namespace = "com.toggl.komposable.sample.todo"

    compileSdk = 33

    defaultConfig {
        applicationId = "com.toggl.komposable.sample.todo"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }
}

dependencies {
    implementation(project(":komposable-architecture"))
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material.core)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(project(":komposable-architecture-test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlin.test.core)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.leak.canary)
}
