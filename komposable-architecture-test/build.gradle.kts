import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

extra.apply {
    set("PUBLISH_GROUP_ID", "com.toggl")
    set("PUBLISH_VERSION", "1.0.0-preview04")
    set("PUBLISH_ARTIFACT_ID", "komposable-architecture-test")
}

apply(from = "${rootProject.projectDir}/scripts/publish-module.gradle")


java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(project(":komposable-architecture"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.junit.jupiter.api)
    implementation(libs.junit.jupiter.engine)
    implementation(libs.kotlin.test.core)
    implementation(libs.kotlin.test.junit5)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotestMatchers)
    implementation(kotlin("reflect"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockK)
    testImplementation(libs.turbine)
    testImplementation(libs.kotestMatchers)
    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test.core)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.addAll(listOf(
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        ))
    }
}
