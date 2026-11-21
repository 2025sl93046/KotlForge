package dev.kotlforge.engine

import dev.kotlforge.core.ArtifactType
import dev.kotlforge.core.BuildArtifact
import dev.kotlforge.core.BuildEngine
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import java.io.File

class GradleRunner(
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) : BuildEngine {

    override fun build(request: BuildRequest): BuildResult {
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

        if (!isWindows() && !wrapper.canExecute()) {
            return BuildResult(
                success = false,
                exitCode = 3,
                durationMillis = 0,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = "Gradle wrapper is not executable: ${wrapper.absolutePath}",
                        suggestion = "Run: chmod +x ${wrapper.absolutePath}"
                    )
                )
            )
        }

        val tasks = resolveGradleTasks(request)
        val result = processRunner.run(
            command = listOf(wrapper.absolutePath) + tasks,
            workingDir = projectDir
        )

        if (result.exitCode != 0) {
            writeRawLog(projectDir, result.stdout, result.stderr)
            val logPath = File(projectDir, "kotlforge-build.log").absolutePath
            return BuildResult(
                success = false,
                exitCode = result.exitCode,
                durationMillis = result.durationMillis,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = "Build failed with exit code ${result.exitCode}",
                        suggestion = "Check $logPath for details"
                    )
                )
            )
        }

        val artifacts = buildList {
            findApks(projectDir, request.variant).forEach { path ->
                val size = File(path).length()
                if (size > 0L) add(BuildArtifact(type = ArtifactType.APK, path = path, sizeBytes = size))
            }
            findAab(projectDir, request.variant)?.let { path ->
                val size = File(path).length()
                if (size > 0L) add(BuildArtifact(type = ArtifactType.AAB, path = path, sizeBytes = size))
            }
        }

        if (artifacts.isEmpty()) {
            val apkCandidates = findApks(projectDir, request.variant)
            val aabPath = findAab(projectDir, request.variant)
            val message = when {
                apkCandidates.isNotEmpty() -> "APK(s) were produced but are empty (0 bytes): ${apkCandidates.joinToString()}"
                aabPath != null -> "AAB was produced but is empty (0 bytes): $aabPath"
                else -> "No APK or AAB found for variant '${request.variant}'"
            }
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
            artifacts = artifacts
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

    internal fun findApks(projectDir: File, variant: String): List<String> {
        val results = mutableListOf<String>()

        // Scan all direct subdirectories: <module>/build/outputs/apk/<variant>/
        projectDir.listFiles()
            ?.filter { it.isDirectory && it.name != "build" }
            ?.forEach { moduleDir ->
                File(moduleDir, "build/outputs/apk/$variant")
                    .walkTopDown()
                    .filter { it.isFile && it.extension == "apk" }
                    .mapTo(results) { it.absolutePath }
            }

        if (results.isNotEmpty()) return results

        // Fallback: scan the whole tree (custom layouts or deeply nested modules)
        projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "apk" && it.path.contains(variant, ignoreCase = true) }
            .mapTo(results) { it.absolutePath }

        return results
    }

    internal fun findAab(projectDir: File, variant: String): String? {
        // Gradle bundle output convention: app/build/outputs/bundle/<variant>/
        val conventional = File(projectDir, "app/build/outputs/bundle/$variant")
            .walkTopDown()
            .filter { it.extension == "aab" }
            .firstOrNull()
        if (conventional != null) return conventional.absolutePath

        // Fallback: scan the whole tree
        return projectDir.walkTopDown()
            .filter { it.extension == "aab" && it.path.contains(variant, ignoreCase = true) }
            .firstOrNull()
            ?.absolutePath
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
