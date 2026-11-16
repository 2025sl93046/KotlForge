plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":diagnostics"))
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}
