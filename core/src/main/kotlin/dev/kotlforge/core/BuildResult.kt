package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class BuildResult(
    val success: Boolean,
    val exitCode: Int,
    val durationMillis: Long,
    val artifacts: List<BuildArtifact> = emptyList(),
    val diagnostics: List<Diagnostic> = emptyList()
)
