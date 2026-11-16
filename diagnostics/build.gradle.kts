plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}
