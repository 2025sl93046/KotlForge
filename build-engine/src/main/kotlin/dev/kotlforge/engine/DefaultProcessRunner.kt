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

        val stdoutBuffer = StringBuilder()
        val stderrBuffer = StringBuilder()

        // Read stdout and stderr concurrently so neither pipe blocks the process
        val stdoutThread = Thread {
            process.inputStream.bufferedReader().forEachLine { line ->
                println(line)
                stdoutBuffer.appendLine(line)
            }
        }.also { it.start() }

        val stderrThread = Thread {
            process.errorStream.bufferedReader().forEachLine { line ->
                System.err.println(line)
                stderrBuffer.appendLine(line)
            }
        }.also { it.start() }

        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        val duration = System.currentTimeMillis() - start

        if (!finished) {
            process.destroyForcibly()
            stdoutThread.join(500)
            stderrThread.join(500)
            return ProcessResult(
                exitCode = -1,
                stdout = stdoutBuffer.toString(),
                stderr = "Process timed out after $timeoutSeconds seconds",
                durationMillis = duration
            )
        }

        stdoutThread.join()
        stderrThread.join()

        return ProcessResult(
            exitCode = process.exitValue(),
            stdout = stdoutBuffer.toString(),
            stderr = stderrBuffer.toString(),
            durationMillis = duration
        )
    }
}
