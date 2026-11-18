package dev.kotlforge.engine

import java.io.File

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMillis: Long
)

interface ProcessRunner {
    fun run(
        command: List<String>,
        workingDir: File,
        timeoutSeconds: Long = 300
    ): ProcessResult
}
