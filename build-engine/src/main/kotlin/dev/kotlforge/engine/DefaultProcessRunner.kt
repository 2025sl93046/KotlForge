package dev.kotlforge.engine

import java.io.File
import java.util.concurrent.TimeUnit

class DefaultProcessRunner : ProcessRunner {
    override fun run(
        command: List<String>,
        workingDir: File,
        timeoutSeconds: Long
    ): ProcessResult {
        val start = System.currentTimeMillis()

        val process = ProcessBuilder(command)
            .directory(workingDir)
            .start()

        val stdoutFuture = process.inputStream.bufferedReader().readText()
        val stderrFuture = process.errorStream.bufferedReader().readText()

        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)

        val duration = System.currentTimeMillis() - start

        if (!finished) {
            process.destroyForcibly()
            return ProcessResult(
                exitCode = -1,
                stdout = stdoutFuture,
                stderr = "Process timed out after $timeoutSeconds seconds",
                durationMillis = duration
            )
        }

        return ProcessResult(
            exitCode = process.exitValue(),
            stdout = stdoutFuture,
            stderr = stderrFuture,
            durationMillis = duration
        )
    }
}
