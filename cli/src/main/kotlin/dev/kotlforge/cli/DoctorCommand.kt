package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.diagnostics.ToolchainDetector
import java.io.File

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Check toolchain requirements (JDK, Android SDK)"
) {
    private val project by option("--project", "-p", help = "Path to Android project root")
        .default(".")

    override fun run() {
        val detector = ToolchainDetector()
        val diagnostics = detector.runChecks(File(project))

        echo("KotlForge Doctor")
        echo("")

        diagnostics.forEach { diagnostic ->
            val label = when (diagnostic.level) {
                DiagnosticLevel.OK      -> "[  OK  ]"
                DiagnosticLevel.WARNING -> "[WARNING]"
                DiagnosticLevel.ERROR   -> "[ ERROR ]"
            }
            echo("  $label  ${diagnostic.message}")
            if (diagnostic.suggestion != null) {
                echo("           → ${diagnostic.suggestion}")
            }
        }

        echo("")

        val errorCount = diagnostics.count { it.level == DiagnosticLevel.ERROR }
        val warnCount  = diagnostics.count { it.level == DiagnosticLevel.WARNING }

        when {
            errorCount > 0 -> {
                val plural = if (errorCount == 1) "error" else "errors"
                echo("  Status: $errorCount $plural found")
                throw ProgramResult(ExitCode.ENV_FAILED)
            }
            warnCount > 0  -> echo("  Status: OK ($warnCount warning(s))")
            else           -> echo("  Status: OK")
        }
    }
}
