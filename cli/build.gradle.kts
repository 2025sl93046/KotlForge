plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serial)
    application
}

application {
    mainClass.set("dev.kotlforge.cli.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":diagnostics"))
    implementation(project(":build-engine"))
    implementation(project(":deploy"))
    implementation(project(":watch"))
    implementation(libs.clikt)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback)
    testImplementation(libs.junit5)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}
