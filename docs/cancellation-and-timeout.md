# Process Cancellation and Timeout Behaviour

This document describes how `DefaultProcessRunner` handles timeouts and what callers should expect
when a Gradle build runs too long or needs to be stopped.

---

## Timeout

Every `ProcessRunner.run()` call accepts a `timeoutSeconds: Long` parameter (default: **300 seconds**).

When the Gradle subprocess does not finish within that window:

1. `process.destroyForcibly()` is called immediately — sends **SIGKILL** on Unix,
   `TerminateProcess` on Windows.
2. The stdout and stderr reader threads are given **500 ms** to drain any buffered output.
3. `ProcessResult` is returned with:
   - `exitCode = -1`
   - `stderr` containing the message `"Process timed out after <N> seconds"`
   - `stdout` reflecting only what was produced before the kill
4. `GradleRunner` writes the truncated output to `kotlforge-build.log` and returns a
   `BuildResult` with `success = false`.

### Choosing a timeout

| Project size | Suggested `timeoutSeconds` |
|---|---|
| Small library (no dependencies) | 60–120 |
| Standard app | 300 (default) |
| Large multi-module project | 600–900 |

The default of 300 seconds is intentionally conservative. Pass an explicit value via
`BuildRequest` tasks or by overriding the `processRunner` to suit your project.

---

## Cancellation

There is currently **no external cancellation handle**. The build process is not interruptible
from outside the `ProcessRunner` while it is running.

The timeout mechanism is the primary way to bound execution time. If you need to stop a build
earlier than the configured timeout, the only option today is to kill the JVM process that hosts
`KotlForge`.

### Implications

- **Graceful shutdown is not guaranteed.** `destroyForcibly()` / SIGKILL does not allow the
  Gradle daemon or any sub-process it spawned to run shutdown hooks or flush write buffers.
- **Output may be truncated.** `kotlforge-build.log` reflects only the stdout/stderr buffered
  before the kill; a partially written log line is possible.
- **The Gradle daemon may survive.** SIGKILL targets the Gradle wrapper process; the Gradle
  daemon it connected to runs in a separate JVM and may keep running until it times out on its
  own (~3 minutes idle by default). This has no effect on correctness but may consume resources.

---

## Exit codes related to timeout

| `exitCode` | Meaning |
|---|---|
| `0` | Process finished successfully |
| Non-zero | Process finished with a Gradle build failure |
| `-1` | Process was killed due to timeout (set by `DefaultProcessRunner`) |

A `-1` exit code will always be accompanied by `"timed out"` in the `stderr` field of
`ProcessResult` and a `DiagnosticLevel.ERROR` entry in `BuildResult.diagnostics`.

---

## Example: setting a custom timeout

```kotlin
val runner = GradleRunner(
    processRunner = DefaultProcessRunner()
)
val result = runner.build(
    BuildRequest(
        projectDir = "/path/to/large-app",
        variant = "release",
        tasks = listOf("assembleRelease")
    )
)
```

`DefaultProcessRunner` uses the `timeoutSeconds` default of **300 s**. To override it, provide
a custom `ProcessRunner` implementation:

```kotlin
class LongTimeoutRunner : ProcessRunner {
    override fun run(command: List<String>, workingDir: File, timeoutSeconds: Long) =
        DefaultProcessRunner().run(command, workingDir, timeoutSeconds = 900)
}

val runner = GradleRunner(processRunner = LongTimeoutRunner())
```
