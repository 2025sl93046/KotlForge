package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class BuildResult(
    val success: Boolean,
    val apkPath: String? = null,
    val errorMessage: String? = null
)
