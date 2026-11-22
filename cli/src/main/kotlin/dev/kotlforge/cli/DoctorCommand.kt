package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.diagnostics.ToolchainDetector
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Check toolchain requirements (JDK, Android SDK)"
) {
    private val format by option("--format", "-f", help = "Output format: text or json")
        .choice("text", "json").default("text")

    private val json = Json { prettyPrint = true }

    override fun run() {
        val detector = ToolchainDetector()
        val info = detector.detect()
        val diagnostics = detector.checks(info)

        val errorCount = diagnostics.count { it.level == DiagnosticLevel.ERROR }
        val warnCount  = diagnostics.count { it.level == DiagnosticLevel.WARNING }

        if (format == "json") {
            val status = when {
                errorCount > 0 -> "error"
                warnCount > 0  -> "warning"
                else           -> "ok"
            }
            echo(json.encodeToString(DoctorReport.serializer(), DoctorReport(status, errorCount, warnCount, diagnostics)))
        } else {
            renderText(diagnostics, errorCount, warnCount)
        }

        if (errorCount > 0) throw ProgramResult(ExitCode.ENV_FAILED)
    }

    private fun renderText(diagnostics: List<Diagnostic>, errorCount: Int, warnCount: Int) {
        echo("KotlForge Doctor")
        echo("")
        diagnostics.forEach { diagnostic ->
            val label = when (diagnostic.level) {
                DiagnosticLevel.OK      -> "[  OK  ]"
                DiagnosticLevel.WARNING -> "[WARNING]"
                DiagnosticLevel.ERROR   -> "[ ERROR ]"
            }
            echo("  $label  ${diagnostic.message}")
            if (diagnostic.suggestion != null) echo("           → ${diagnostic.suggestion}")
        }
        echo("")
        when {
            errorCount > 0 -> echo("  Status: $errorCount ${if (errorCount == 1) "error" else "errors"} found")
            warnCount > 0  -> echo("  Status: OK ($warnCount warning(s))")
            else           -> echo("  Status: OK")
        }
    }
}

@Serializable
data class DoctorReport(
    val status: String,
    val errorCount: Int,
    val warningCount: Int,
    val checks: List<Diagnostic>
)
