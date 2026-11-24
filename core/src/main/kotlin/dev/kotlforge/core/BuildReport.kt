package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class BuildReport(
    val schemaVersion: String = "1.0",
    val timestamp: String,
    val projectRoot: String,
    val variant: String,
    val success: Boolean,
    val durationMillis: Long,
    val artifacts: List<BuildArtifact> = emptyList(),
    val diagnostics: List<Diagnostic> = emptyList()
)
