plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("dev.kotlforge.cli.MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":diagnostics"))
    implementation(project(":build-engine"))
    implementation(libs.clikt)
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback)
    testImplementation(libs.junit5)
}
