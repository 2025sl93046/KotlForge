package dev.kotlforge.engine

import dev.kotlforge.core.ArtifactType
import dev.kotlforge.core.BuildArtifact
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import java.io.File

class GradleRunner(
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun run(request: BuildRequest): BuildResult {
        val projectDir = File(request.projectDir)
        val wrapper = locateWrapper(projectDir)
            ?: return BuildResult(
                success = false,
                exitCode = 3,
                durationMillis = 0,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = "Gradle wrapper not found in ${projectDir.absolutePath}",
                        suggestion = "Make sure the project has a gradlew file at its root"
                    )
                )
            )

        val tasks = resolveGradleTasks(request)
        val result = processRunner.run(
            command = listOf(wrapper.absolutePath) + tasks,
            workingDir = projectDir
        )

        writeRawLog(projectDir, result.stdout, result.stderr)

        if (result.exitCode != 0) {
            return BuildResult(
                success = false,
                exitCode = result.exitCode,
                durationMillis = result.durationMillis,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = "Build failed with exit code ${result.exitCode}",
                        suggestion = "Check kotlforge-build.log in the project directory for details"
                    )
                )
            )
        }

        val apk = findApk(projectDir, request.variant)
        val apkFile = apk?.let { File(it) }
        val apkSize = apkFile?.length() ?: 0L

        if (apkFile == null || apkSize == 0L) {
            val message = if (apkFile != null) "APK was produced but is empty (0 bytes): $apk"
                          else "No APK found for variant '${request.variant}'"
            return BuildResult(
                success = false,
                exitCode = result.exitCode,
                durationMillis = result.durationMillis,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = message,
                        suggestion = "Run the build manually and check the Gradle output for errors"
                    )
                )
            )
        }

        return BuildResult(
            success = true,
            exitCode = result.exitCode,
            durationMillis = result.durationMillis,
            artifacts = listOf(BuildArtifact(type = ArtifactType.APK, path = apk, sizeBytes = apkSize))
        )
    }

    internal fun writeRawLog(projectDir: File, stdout: String, stderr: String) {
        val logFile = File(projectDir, "kotlforge-build.log")
        logFile.writeText(buildString {
            if (stdout.isNotBlank()) {
                appendLine("=== STDOUT ===")
                appendLine(stdout)
            }
            if (stderr.isNotBlank()) {
                appendLine("=== STDERR ===")
                appendLine(stderr)
            }
        })
    }

    internal fun resolveGradleTasks(request: BuildRequest): List<String> {
        if (request.tasks.isNotEmpty()) return request.tasks
        val variant = request.variant.replaceFirstChar { it.uppercaseChar() }
        return listOf("assemble$variant")
    }

    internal fun locateWrapper(projectDir: File): File? {
        val name = if (isWindows()) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, name)
        return if (wrapper.exists() && wrapper.isFile) wrapper else null
    }

    private fun findApk(projectDir: File, variant: String): String? {
        // Gradle standard output convention: app/build/outputs/apk/<variant>/
        val conventional = File(projectDir, "app/build/outputs/apk/$variant")
            .walkTopDown()
            .filter { it.extension == "apk" }
            .firstOrNull()
        if (conventional != null) return conventional.absolutePath

        // Fallback: scan the whole tree (multi-module or custom layouts)
        return projectDir.walkTopDown()
            .filter { it.extension == "apk" && it.path.contains(variant, ignoreCase = true) }
            .firstOrNull()
            ?.absolutePath
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
