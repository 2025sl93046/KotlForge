package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class BuildRequest(
    val projectPath: String,
    val variant: String = "debug"
)
