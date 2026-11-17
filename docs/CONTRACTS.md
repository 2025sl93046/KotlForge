# KotlForge — Team Contracts

This document records the agreed shared interfaces across all four modules.
**No module should define its own version of these types.**
All changes must go through a PR reviewed by all four students.

---

## Status

| Contract | Proposed by | Status | PR |
|---|---|---|---|
| 7.1 BuildRequest / BuildResult | Student 1 | Proposed | — |
| 7.2 ToolchainInfo / Diagnostic | Student 2 | Proposed | — |
| 7.3 ProcessRunner interface | Student 3 | Proposed | — |
| 7.4 CLI commands and exit codes | Student 4 | Proposed | — |
| 7.5 JSON report schema | Student 4 + Student 1 | Proposed | — |

> Update this table when a contract is approved and merged.
> Status: `Proposed` → `In Review` → `Agreed` → `Implemented`

---

## 7.1 — BuildRequest / BuildResult

**Owner:** Student 1 | **Module:** `core`

### BuildRequest

Represents a request to build an Android project.

```kotlin
data class BuildRequest(
    val projectDir: Path,
    val variant: String = "debug",
    val tasks: List<String> = emptyList()
)
```

| Field | Type | Default | Description |
|---|---|---|---|
| `projectDir` | `Path` | required | Absolute path to the Android project root |
| `variant` | `String` | `"debug"` | Build variant — `debug`, `release`, or custom |
| `tasks` | `List<String>` | `[]` | Optional explicit Gradle tasks to run |

### BuildResult

Returned after every build attempt, success or failure.

```kotlin
data class BuildResult(
    val success: Boolean,
    val exitCode: Int,
    val durationMillis: Long,
    val artifacts: List<BuildArtifact>,
    val diagnostics: List<Diagnostic>
)
```

| Field | Type | Description |
|---|---|---|
| `success` | `Boolean` | `true` only if exit code is 0 and at least one artifact exists |
| `exitCode` | `Int` | Raw process exit code from Gradle |
| `durationMillis` | `Long` | Wall-clock time from process start to finish |
| `artifacts` | `List<BuildArtifact>` | Located output files (APK, AAB) |
| `diagnostics` | `List<Diagnostic>` | Warnings and errors from toolchain and build |

### BuildArtifact

Describes a single output file produced by the build.

```kotlin
data class BuildArtifact(
    val type: ArtifactType,
    val path: String,
    val sizeBytes: Long
)

enum class ArtifactType { APK, AAB }
```

### BuildEngine interface

The contract that `build-engine` implements and `core` depends on.

```kotlin
interface BuildEngine {
    fun build(request: BuildRequest): BuildResult
}
```

---

## 7.2 — ToolchainInfo / Diagnostic

**Owner:** Student 2 | **Module:** `diagnostics` → defined in `core`

### ToolchainInfo

Snapshot of the local development environment.

```kotlin
data class ToolchainInfo(
    val jdkPath: String?,
    val jdkVersion: String?,
    val androidSdkPath: String?,
    val gradleVersion: String?,
    val agpVersion: String?
)
```

| Field | Type | Description |
|---|---|---|
| `jdkPath` | `String?` | Value of `JAVA_HOME`, or `null` if not set |
| `jdkVersion` | `String?` | Parsed from `java -version` output |
| `androidSdkPath` | `String?` | Value of `ANDROID_HOME` or `ANDROID_SDK_ROOT` |
| `gradleVersion` | `String?` | Parsed from `gradle-wrapper.properties` |
| `agpVersion` | `String?` | Parsed from project build config |

### Diagnostic

A single check result from the toolchain detector.

```kotlin
data class Diagnostic(
    val level: DiagnosticLevel,
    val message: String,
    val suggestion: String? = null
)

enum class DiagnosticLevel { OK, WARNING, ERROR }
```

| Field | Type | Description |
|---|---|---|
| `level` | `DiagnosticLevel` | Severity: OK / WARNING / ERROR |
| `message` | `String` | Human-readable description of what was found |
| `suggestion` | `String?` | Actionable next step — never null for ERROR level |

**Rules:**
- `ERROR` diagnostics must always include a `suggestion`
- `WARNING` diagnostics should include a `suggestion` where possible
- No secret values (tokens, passwords, paths containing credentials) may appear in any field

---

## 7.3 — ProcessRunner Interface

**Owner:** Student 3 | **Module:** `build-engine`

Abstracts process execution so it can be mocked in tests.

```kotlin
interface ProcessRunner {
    fun run(
        command: List<String>,
        workingDir: File,
        timeoutSeconds: Long = 300
    ): ProcessResult
}

data class ProcessResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMillis: Long
)
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `command` | `List<String>` | required | Full command including executable and arguments |
| `workingDir` | `File` | required | Directory to run the process in |
| `timeoutSeconds` | `Long` | `300` | Kill process after this many seconds |

**Rules:**
- Never embed passwords or signing secrets in `command` arguments
- On timeout: kill the process, set `exitCode = -1`, include timeout message in `stderr`
- On cancellation: same behavior as timeout
- `stdout` and `stderr` are captured in full; also stream to console in real time

---

## 7.4 — CLI Commands and Exit Codes

**Owner:** Student 4 | **Module:** `cli`

### Commands

| Command | Description | Key flags |
|---|---|---|
| `doctor` | Check toolchain requirements | `--format` |
| `inspect` | Show project info without building | `--project`, `--format` |
| `build` | Build the Android project | `--project`, `--variant`, `--format` |
| `clean` | Run Gradle clean on the project | `--project` |
| `init` | Generate a new minimal Android project | `--name`, `--output` |

### Global flags

| Flag | Short | Default | Description |
|---|---|---|---|
| `--project` | `-p` | `.` | Path to the Android project |
| `--variant` | `-v` | `debug` | Build variant |
| `--format` | — | `text` | Output format: `text` or `json` |

### Exit codes

| Code | Meaning | When |
|---|---|---|
| `0` | Success | Build succeeded, artifact found |
| `1` | Build failure | Gradle exited non-zero |
| `2` | Invalid arguments | Bad flags, missing required option |
| `3` | Environment failure | Preflight / doctor check failed |

**Rules:**
- Every command must respect `--format json` and produce valid JSON to stdout
- Exit codes must be consistent — never exit `0` if an artifact is missing
- Help text (`--help`) must be available on every command

---

## 7.5 — JSON Report Schema v1

**Owner:** Student 4 + Student 1 | **Written to:** `kotlforge-report.json`

### Schema

```json
{
  "schemaVersion": "1",
  "status": "SUCCESS",
  "variant": "debug",
  "durationMillis": 18400,
  "artifacts": [
    {
      "type": "APK",
      "path": "app/build/outputs/apk/debug/app-debug.apk",
      "sizeBytes": 2457600
    }
  ],
  "diagnostics": [
    {
      "level": "WARNING",
      "message": "Android SDK platform 34 not found",
      "suggestion": "Run: sdkmanager 'platforms;android-34'"
    }
  ]
}
```

### Field reference

| Field | Type | Description |
|---|---|---|
| `schemaVersion` | `String` | Always `"1"` for this release — increment on breaking change |
| `status` | `String` | `SUCCESS` or `FAILURE` |
| `variant` | `String` | The requested build variant |
| `durationMillis` | `Long` | Total build wall-clock time |
| `artifacts` | `Array` | List of produced files |
| `artifacts[].type` | `String` | `APK` or `AAB` |
| `artifacts[].path` | `String` | Relative path from project root |
| `artifacts[].sizeBytes` | `Long` | File size in bytes |
| `diagnostics` | `Array` | Toolchain and build warnings/errors |
| `diagnostics[].level` | `String` | `OK`, `WARNING`, or `ERROR` |
| `diagnostics[].message` | `String` | Human-readable description |
| `diagnostics[].suggestion` | `String?` | Next action — present for all ERROR entries |

### Evolution rules

1. Adding new optional fields is backward-compatible — allowed without version bump
2. Removing or renaming fields requires incrementing `schemaVersion`
3. CI systems should check `schemaVersion` before parsing
4. Never include secret values, credentials or signing keys in this file

---

## How to propose a change

1. Open a GitHub Discussion tagged `contracts`
2. State which contract (7.1–7.5), what you want to change and why
3. All four students must comment ✅ before a PR is opened
4. PR must update both this file and the Kotlin source in `core/`
5. Merge only after all four approve

---

## Module import rules

```
cli         → may import core, diagnostics, build-engine
build-engine → may import core only
diagnostics  → may import core only
core         → no internal imports
```

No module may import from `cli`. No circular dependencies.
