package dev.kotlforge.core

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

object ArtifactValidator {

    private val APK_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    fun validate(artifact: BuildArtifact): List<Diagnostic> {
        val path = Path.of(artifact.path)
        val diagnostics = mutableListOf<Diagnostic>()

        if (!path.exists()) {
            diagnostics += Diagnostic(
                level      = DiagnosticLevel.ERROR,
                message    = "Artifact not found: ${artifact.path}",
                suggestion = "Check that the build succeeded and the variant is correct"
            )
            return diagnostics
        }

        val actualSize = path.fileSize()
        if (actualSize == 0L) {
            diagnostics += Diagnostic(
                level      = DiagnosticLevel.ERROR,
                message    = "Artifact is empty (0 bytes): ${artifact.path}",
                suggestion = "The build may have produced a corrupt output — try a clean build"
            )
        }

        if (artifact.type == ArtifactType.APK && !hasZipMagic(path)) {
            diagnostics += Diagnostic(
                level      = DiagnosticLevel.ERROR,
                message    = "Artifact is not a valid APK (bad magic bytes): ${artifact.path}",
                suggestion = "The APK may be corrupt — try a clean build"
            )
        }

        if (diagnostics.isEmpty()) {
            diagnostics += Diagnostic(
                level   = DiagnosticLevel.OK,
                message = "Artifact OK: ${artifact.path} (${actualSize / 1024}KB)"
            )
        }

        return diagnostics
    }

    private fun hasZipMagic(path: Path): Boolean {
        return try {
            val header = ByteArray(4)
            path.inputStream().use { it.read(header) }
            header.contentEquals(APK_MAGIC)
        } catch (_: Exception) {
            false
        }
    }
}
