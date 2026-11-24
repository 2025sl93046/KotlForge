package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.engine.GradleRunner
import java.io.File
import java.nio.file.Path

class CleanCommand : CliktCommand(
    name = "clean",
    help = "Delete all build outputs for the project"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")

    override fun run() {
        val absRoot = Path.of(projectPath).toAbsolutePath().normalize()
        val projectDir = absRoot.toFile()

        echo("Cleaning project at: $absRoot")

        val result = GradleRunner().build(
            BuildRequest(projectDir = absRoot.toString(), tasks = listOf("clean"))
        )

        if (!result.success) {
            val error = result.diagnostics.firstOrNull()
            echo("Clean failed: ${error?.message ?: "unknown error"}", err = true)
            if (error?.suggestion != null) echo("  Suggestion: ${error.suggestion}", err = true)
            throw ProgramResult(ExitCode.BUILD_FAILED)
        }

        deleteKotlforgeFiles(projectDir)

        echo("Clean successful")
    }

    private fun deleteKotlforgeFiles(projectDir: File) {
        listOf("kotlforge-report.json", "kotlforge-build.log").forEach { name ->
            val file = File(projectDir, name)
            if (file.exists()) {
                file.delete()
                echo("Deleted: ${file.name}")
            }
        }
    }
}
