plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serial)
}

dependencies {
    implementation(libs.kotlinx.serialization)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}
