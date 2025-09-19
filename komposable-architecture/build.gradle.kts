import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
}

extra.apply {
    set("PUBLISH_GROUP_ID", "com.toggl")
    set("PUBLISH_VERSION", "1.0.0-preview04")
    set("PUBLISH_ARTIFACT_ID", "komposable-architecture")
}

apply(from = "${rootProject.projectDir}/scripts/publish-module.gradle")


java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(project(":komposable-architecture-test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockK)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.matchers)

    testImplementation(project(":komposable-architecture-test"))
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
