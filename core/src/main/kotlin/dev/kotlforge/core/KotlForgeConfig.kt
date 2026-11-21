package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class KotlForgeConfig(
    val defaultVariant: String = "debug",
    val defaultTasks: List<String> = emptyList(),
    val jdkPath: String? = null
)
