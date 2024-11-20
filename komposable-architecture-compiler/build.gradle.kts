plugins {
    alias(libs.plugins.kotlin.jvm)
}

extra.apply {
    set("PUBLISH_GROUP_ID", "com.toggl")
    set("PUBLISH_VERSION", "1.0.0-preview04")
    set("PUBLISH_ARTIFACT_ID", "komposable-architecture-compiler")
}

apply(from = "${rootProject.projectDir}/scripts/publish-module.gradle")


java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(project(":komposable-architecture"))
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet.ksp)


    testImplementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test.core)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testImplementation(libs.kotestMatchers)
}
