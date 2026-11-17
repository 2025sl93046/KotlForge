package dev.kotlforge.core

import kotlinx.serialization.Serializable

enum class ArtifactType { APK, AAB }

@Serializable
data class BuildArtifact(
    val type: ArtifactType,
    val path: String,
    val sizeBytes: Long
)
