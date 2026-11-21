package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.diagnostics.ToolchainDetector

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Check toolchain requirements (JDK, Android SDK)"
) {
    override fun run() {
        val detector = ToolchainDetector()
        val info = detector.detect()
        val diagnostics = detector.checks(info)

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
