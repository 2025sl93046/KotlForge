plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":build-engine"))
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}
