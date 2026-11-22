package dev.kotlforge.core

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream

object ArtifactValidator {

    private const val MAGIC_HEADER_SIZE = 4
    private const val BYTES_PER_KB = 1024L
    private const val ZIP_MAGIC_BYTE_0 = 0x50.toByte()
    private const val ZIP_MAGIC_BYTE_1 = 0x4B.toByte()
    private const val ZIP_MAGIC_BYTE_2 = 0x03.toByte()
    private const val ZIP_MAGIC_BYTE_3 = 0x04.toByte()
    private val APK_MAGIC = byteArrayOf(ZIP_MAGIC_BYTE_0, ZIP_MAGIC_BYTE_1, ZIP_MAGIC_BYTE_2, ZIP_MAGIC_BYTE_3)

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
                message = "Artifact OK: ${artifact.path} (${actualSize / BYTES_PER_KB}KB)"
            )
        }

        return diagnostics
    }

    private fun hasZipMagic(path: Path): Boolean {
        return try {
            val header = ByteArray(MAGIC_HEADER_SIZE)
            path.inputStream().use { it.read(header) }
            header.contentEquals(APK_MAGIC)
        } catch (_: Exception) {
            false
        }
    }
}
