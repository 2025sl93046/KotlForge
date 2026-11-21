package dev.kotlforge.core

import kotlinx.serialization.Serializable

@Serializable
data class KotlForgeConfig(
    val defaultVariant: String = "debug",
    val defaultTasks: List<String> = emptyList(),
    val jdkPath: String? = null,
    val watchPaths: List<String> = listOf("src"),
    val watchIgnorePatterns: List<String> = listOf("**/*.class", "**/build/**", "**/.gradle/**", "**/.git/**")
)
