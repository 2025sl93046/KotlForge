package dev.kotlforge.engine

import dev.kotlforge.core.ArtifactType
import dev.kotlforge.core.BuildArtifact
import dev.kotlforge.core.BuildEngine
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import java.io.File

class GradleRunner : BuildEngine {

    override fun build(request: BuildRequest): BuildResult {
        val projectDir = File(request.projectDir)
        val wrapper = File(projectDir, if (isWindows()) "gradlew.bat" else "gradlew")
        val start = System.currentTimeMillis()

        if (!wrapper.exists()) {
            return BuildResult(
                success = false,
                exitCode = 3,
                durationMillis = 0,
                diagnostics = listOf(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Gradle wrapper not found at ${wrapper.absolutePath}",
                    suggestion = "Make sure the project has a gradlew file at its root"
                ))
            )
        }

        val pb = ProcessBuilder(wrapper.absolutePath, "assemble${request.variant.replaceFirstChar { it.uppercaseChar() }}")
            .directory(projectDir)
            .redirectErrorStream(true)

        pb.environment()["ANDROID_HOME"] = System.getenv("ANDROID_HOME") ?: ""
        System.getenv("JAVA_HOME")?.let { pb.environment()["JAVA_HOME"] = it }

        val process = pb.start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        val duration = System.currentTimeMillis() - start

        if (exitCode != 0) {
            println(output)
            return BuildResult(
                success = false,
                exitCode = exitCode,
                durationMillis = duration,
                diagnostics = listOf(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Build failed with exit code $exitCode",
                    suggestion = output.lines().lastOrNull { it.isNotBlank() } ?: "Check the Gradle output for details"
                ))
            )
        }

        val apk = findApk(projectDir, request.variant)
        val artifacts = if (apk != null) {
            listOf(BuildArtifact(type = ArtifactType.APK, path = apk, sizeBytes = File(apk).length()))
        } else emptyList()

        return BuildResult(
            success = apk != null,
            exitCode = exitCode,
            durationMillis = duration,
            artifacts = artifacts
        )
    }

    private fun findApk(projectDir: File, variant: String): String? {
        return projectDir.walkTopDown()
            .filter { it.name.endsWith(".apk") && it.path.contains(variant, ignoreCase = true) }
            .firstOrNull()
            ?.absolutePath
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
