package dev.kotlforge.core

import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class BuildRequest(
    val projectDir: String,
    val variant: String = "debug",
    val tasks: List<String> = emptyList()
) {
    fun resolvedProjectDir(): Path = Path.of(projectDir)
}
