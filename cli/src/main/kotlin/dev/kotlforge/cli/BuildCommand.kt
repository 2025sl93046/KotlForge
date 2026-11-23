package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.kotlforge.core.BuildReport
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import dev.kotlforge.engine.GradleRunner
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json

class BuildCommand : CliktCommand(
    name = "build",
    help = "Build an Android project"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")
    private val variant    by option("--variant",  "-v", help = "Build variant (debug/release)").default("debug")
    private val format     by option("--format",   "-f", help = "Output format: text or json").choice("text", "json").default("text")

    private val json = Json { prettyPrint = true }

    override fun run() {
        if (format == "text") echo("Building $variant variant at: $projectPath")

        val absRoot = Path.of(projectPath).toAbsolutePath().normalize()
        val result = GradleRunner().build(BuildRequest(projectDir = projectPath, variant = variant))

        writeReport(absRoot, result)

        when (format) {
            "json" -> echo(json.encodeToString(BuildResult.serializer(), result))
            else   -> renderText(result)
        }

        if (!result.success) throw ProgramResult(ExitCode.BUILD_FAILED)
    }

    private fun writeReport(projectRoot: Path, result: BuildResult) {
        val report = BuildReport(
            timestamp      = Instant.now().toString(),
            projectRoot    = projectRoot.toString(),
            variant        = variant,
            success        = result.success,
            durationMillis = result.durationMillis,
            artifacts      = result.artifacts,
            diagnostics    = result.diagnostics
        )
        val reportFile = projectRoot.resolve("kotlforge-report.json")
        try {
            reportFile.writeText(json.encodeToString(BuildReport.serializer(), report))
            if (format == "text") echo("Report written: $reportFile")
        } catch (e: IOException) {
            if (format == "text") echo("Warning: could not write report — ${e.message}")
        }
    }

    private fun renderText(result: BuildResult) {
        if (result.success) {
            echo("Build successful (${result.durationMillis}ms)")
            result.artifacts.forEach { artifact ->
                echo("  APK: ${artifact.path} (${artifact.sizeBytes / 1024}KB)")
            }
        } else {
            val error = result.diagnostics.firstOrNull()
            echo("Build failed (exit ${result.exitCode}): ${error?.message ?: "unknown error"}")
            if (error?.suggestion != null) echo("Suggestion: ${error.suggestion}")
        }
    }
} 
