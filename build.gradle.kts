import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.kotlin.jvm)    apply false
    alias(libs.plugins.kotlin.serial) apply false
    alias(libs.plugins.detekt)        apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files(".github/detekt.yml"))
        buildUponDefaultConfig = true
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
