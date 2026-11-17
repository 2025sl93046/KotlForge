package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class ToolchainInfo(
    val jdkPath: String? = null,
    val jdkVersion: String? = null,
    val androidSdkPath: String? = null,
    val gradleVersion: String? = null,
    val agpVersion: String? = null
)
