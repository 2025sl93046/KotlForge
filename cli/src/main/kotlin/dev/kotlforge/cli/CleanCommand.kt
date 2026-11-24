package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
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

        val wrapperName = if (isWindows()) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, wrapperName)

        if (!wrapper.exists()) {
            echo("Error: Gradle wrapper not found in ${projectDir.absolutePath}", err = true)
            throw ProgramResult(ExitCode.BAD_ARGS)
        }

        echo("Cleaning project at: $absRoot")

        val process = ProcessBuilder(wrapper.absolutePath, "clean")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        process.inputStream.bufferedReader().forEachLine { echo(it) }

        val exitCode = process.waitFor()
        if (exitCode != 0) {
            echo("Clean failed", err = true)
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

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
