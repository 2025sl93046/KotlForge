plugins {
    alias(libs.plugins.kotlin.jvm)    apply false
    alias(libs.plugins.kotlin.serial) apply false
    alias(libs.plugins.detekt)        apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
