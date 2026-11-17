package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.engine.GradleRunner

class BuildCommand : CliktCommand(
    name = "build",
    help = "Build an Android project"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")
    private val variant by option("--variant", "-v", help = "Build variant (debug/release)").default("debug")

    override fun run() {
        echo("Building $variant variant at: $projectPath")
        val result = GradleRunner().run(BuildRequest(projectDir = projectPath, variant = variant))
        if (result.success) {
            echo("Build successful")
            result.artifacts.forEach { echo("APK: ${it.path}") }
        } else {
            val error = result.diagnostics.firstOrNull()
            echo("Build failed: ${error?.message ?: "unknown error"}")
        }
    }
}
