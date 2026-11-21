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

        val task = "assemble${request.variant.replaceFirstChar { it.uppercaseChar() }}"
        val result = processRunner.run(
            command = listOf(wrapper.absolutePath, task),
            workingDir = projectDir
        )

        if (result.exitCode != 0) {
            return BuildResult(
                success = false,
                exitCode = result.exitCode,
                durationMillis = result.durationMillis,
                diagnostics = listOf(
                    Diagnostic(
                        level = DiagnosticLevel.ERROR,
                        message = "Build failed with exit code ${result.exitCode}",
                        suggestion = "Check the Gradle output for details"
                    )
                )
            )
        }

        val apk = findApk(projectDir, request.variant)
        val artifacts = if (apk != null) {
            listOf(BuildArtifact(type = ArtifactType.APK, path = apk, sizeBytes = File(apk).length()))
        } else emptyList()

        return BuildResult(
            success = apk != null,
            exitCode = result.exitCode,
            durationMillis = result.durationMillis,
            artifacts = artifacts
        )
    }

    internal fun locateWrapper(projectDir: File): File? {
        val name = if (isWindows()) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, name)
        return if (wrapper.exists() && wrapper.isFile) wrapper else null
    }

    private fun findApk(projectDir: File, variant: String): String? {
        return projectDir.walkTopDown()
            .filter { it.name.endsWith(".apk") && it.path.contains(variant, ignoreCase = true) }
            .firstOrNull()
            ?.absolutePath
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
