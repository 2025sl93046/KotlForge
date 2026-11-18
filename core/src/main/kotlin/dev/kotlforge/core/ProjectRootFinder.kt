package dev.kotlforge.core

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object ProjectRootFinder {

    private val SETTINGS_FILES = listOf("settings.gradle.kts", "settings.gradle")

    fun find(startingDir: Path): Path? {
        var current: Path? = startingDir.toAbsolutePath().normalize()
        while (current != null) {
            if (current.isDirectory() && SETTINGS_FILES.any { current.resolve(it).exists() }) {
                return current
            }
            current = current.parent
        }
        return null
    }
}
