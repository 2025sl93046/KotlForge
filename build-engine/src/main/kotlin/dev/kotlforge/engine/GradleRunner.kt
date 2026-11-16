package dev.kotlforge.engine

import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import java.io.File

class GradleRunner {

    fun run(request: BuildRequest): BuildResult {
        val projectDir = File(request.projectPath)
        val wrapper = File(projectDir, if (isWindows()) "gradlew.bat" else "gradlew")

        if (!wrapper.exists()) {
            return BuildResult(success = false, errorMessage = "Gradle wrapper not found at ${wrapper.absolutePath}")
        }

        val process = ProcessBuilder(wrapper.absolutePath, "assemble${request.variant.capitalize()}")
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            return BuildResult(success = false, errorMessage = output)
        }

        val apk = findApk(projectDir, request.variant)
        return BuildResult(success = true, apkPath = apk)
    }

    private fun findApk(projectDir: File, variant: String): String? {
        return projectDir.walkTopDown()
            .filter { it.name.endsWith(".apk") && it.path.contains(variant, ignoreCase = true) }
            .firstOrNull()
            ?.absolutePath
    }

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
