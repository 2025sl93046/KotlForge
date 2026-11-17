package dev.kotlforge.core

import kotlinx.serialization.Serializable

enum class DiagnosticLevel { OK, WARNING, ERROR }

@Serializable
data class Diagnostic(
    val level: DiagnosticLevel,
    val message: String,
    val suggestion: String? = null
)
