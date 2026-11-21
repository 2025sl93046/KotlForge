plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serial)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":build-engine"))
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}
