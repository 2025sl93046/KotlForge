# KotlForge — Epics & Backlog

An epic is a large unit of work with a clear goal and a demonstrable outcome. Each epic maps to one module and one team member as the primary owner. Epics are broken into smaller Backlog Items (BLIs) that can be picked up, reviewed, and merged independently.

---

## Epic 7 — Team Contracts ⚠️ Do first

> Must be completed before any other epic begins. Nothing else can be built until these contracts are agreed and merged.

These BLIs define the shared interfaces across all modules. See `docs/CONTRACTS.md` for the full agreed text.

| BLI | Title | Priority |
|---|---|---|
| 7.1 | Agree `BuildRequest` / `BuildResult` contracts | Critical |
| 7.2 | Agree `ToolchainInfo` / `Diagnostic` contracts | Critical |
| 7.3 | Agree process execution interface | Critical |
| 7.4 | Agree CLI commands and exit codes | Critical |
| 7.5 | Agree JSON report schema v1 | Critical |

---

## Epic 1 — Core API & Data Models

**Module:** `core` | **Milestone:** MVP 0.1

Define the stable contracts that every other module depends on. A Kotlin program should be able to invoke KotlForge and receive a typed `BuildResult`.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 1.1 | Define `BuildRequest` data model | Critical | 2 | 7.1 |
| 1.2 | Define `BuildResult` and `BuildArtifact` models | Critical | 2 | 7.1 |
| 1.3 | Define `Diagnostic` model | Critical | 2 | 7.2 |
| 1.4 | Define `ToolchainInfo` model | Critical | 2 | 7.2 |
| 1.5 | Define `BuildEngine` interface | High | 3 | 1.1, 1.2 |
| 1.6 | Implement `KotlForge` facade / service API | High | 3 | 1.5 |
| 1.7 | Implement project-root discovery | High | 3 | 1.6 |
| 1.8 | Implement configuration loading and validation | High | 2 | 1.6 |
| 1.9 | Serialization tests for all core models | High | 2 | 1.1–1.4 |
| 1.10 | API contract tests — core usable without CLI | High | 2 | 1.6 |
| 1.11 | Sample Kotlin program that calls the library | Medium | 2 | 1.6 |
| 1.12 | Document API stability, error model and cancellation | Medium | 2 | 1.6 |

**Total: 27 story points**

---

## Epic 2 — Toolchain Detection & Diagnostics

**Module:** `diagnostics` | **Milestone:** MVP 0.1

Make environment problems visible before they become confusing build failures. `kotlforge doctor` should report environment state with actionable OK/WARNING/ERROR messages.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 2.1 | Detect JDK — vendor, version, path | Critical | 3 | 7.2 |
| 2.2 | Detect Android SDK from env vars and standard paths | Critical | 3 | 7.2 |
| 2.3 | Check required SDK platforms and build-tools packages | High | 3 | 2.2 |
| 2.4 | Read Gradle Wrapper version from project | High | 2 | — |
| 2.5 | Detect AGP and Kotlin plugin versions from build config | High | 3 | 2.4 |
| 2.6 | Produce structured OK/WARNING/ERROR checks with next action | High | 3 | 2.1–2.5 |
| 2.7 | Ensure no credentials or private env values are printed | Critical | 1 | 2.6 |
| 2.8 | Unit tests with injected env/path fixtures | High | 3 | 2.6 |
| 2.9 | Test: missing SDK, invalid JAVA_HOME, missing wrapper | High | 2 | 2.8 |
| 2.10 | Document tested compatibility matrix | Medium | 2 | 2.5 |
| 2.11 | Windows SDK path discovery | Medium | 2 | 2.2 |
| 2.12 | Compatibility warnings for known bad version combos | Medium | 3 | 2.5 |

**Total: 30 story points**

---

## Epic 3 — Build Engine & Gradle Integration

**Module:** `build-engine` | **Milestone:** MVP 0.1

Reliably execute the official Android build pipeline for existing projects. An existing project should produce a debug APK through KotlForge.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 3.1 | Implement `ProcessRunner` abstraction (stdout, stderr, exit code, cancellation) | Critical | 5 | 7.3, 1.5 |
| 3.2 | Locate and invoke `./gradlew` / `gradlew.bat` | Critical | 3 | 3.1 |
| 3.3 | Map `BuildRequest` variant to Gradle task | High | 2 | 3.2 |
| 3.4 | Stream and capture stdout/stderr during build | High | 2 | 3.1 |
| 3.5 | Capture build duration and write raw logs | Medium | 2 | 3.2 |
| 3.6 | Discover APK outputs using Gradle output conventions | High | 3 | 3.3 |
| 3.7 | Validate APK exists and is non-empty | High | 2 | 3.6 |
| 3.8 | Return failed `BuildResult` with exit code and log reference | High | 2 | 3.6 |
| 3.9 | Handle missing `gradlew` gracefully with clear error | High | 1 | 3.2 |
| 3.10 | Unit tests with mocked process execution | High | 3 | 3.1 |
| 3.11 | Test paths with spaces and platform differences | Medium | 2 | 3.2 |
| 3.12 | Document cancellation and timeout behavior | Medium | 1 | 3.1 |
| 3.13 | AAB output discovery (`bundleDebug` / `bundleRelease`) | Medium | 3 | 3.6 |
| 3.14 | Multi-module APK discovery | Medium | 3 | 3.6 |

**Total: 34 story points**

---

## Epic 4 — CLI, Reporting & Artifact Validation

**Module:** `cli` | **Milestone:** MVP 0.1

Make KotlForge easy to invoke for both people and CI automation. The CLI and GitHub Actions should publish build status, report, and APK.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 4.1 | Define stable exit codes (0=success, 1=build fail, 2=bad args, 3=env fail) | Critical | 1 | 7.4 |
| 4.2 | Implement `doctor` command — render toolchain checks | Critical | 3 | 2.6 |
| 4.3 | Implement `build` command — `--project`, `--variant`, `--format` | Critical | 3 | 3.8 |
| 4.4 | Implement `inspect` command — project info without building | High | 2 | 1.7 |
| 4.5 | Implement `--format json` flag across all commands | High | 2 | 4.1 |
| 4.6 | Define and implement JSON report schema v1 | Critical | 3 | 3.7, 7.5 |
| 4.7 | Write `kotlforge-report.json` to project directory after build | High | 2 | 4.6 |
| 4.8 | Validate artifact existence, file size and basic APK metadata | High | 3 | 3.7 |
| 4.9 | CLI tests — valid commands, invalid args, exit codes | High | 3 | 4.3 |
| 4.10 | Document JSON schema for backward-compatible evolution | Medium | 2 | 4.6 |
| 4.11 | Document Jenkins/GitLab/Bitrise CLI invocation guide | Medium | 2 | 4.3 |
| 4.12 | Implement `clean` command | Medium | 2 | 4.3 |
| 4.13 | Implement `init` command (project generator) | Medium | 5 | 1.7 |

**Total: 33 story points**

---

## Epic 5 — Sample App & CI Integration

**Module:** `samples` + `.github` | **Milestone:** MVP 0.1

Prove KotlForge builds a real Android project end to end in CI.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 5.1 | Add minimal Android app under `samples/android-app` | Critical | 3 | — |
| 5.2 | Complete `integration.yml` — build sample app, upload APK + report | Critical | 3 | 4.7, 5.1 |
| 5.3 | Verify APK is non-empty and path correct in CI | Critical | 2 | 5.2 |
| 5.4 | Trigger controlled failure in CI and verify non-zero exit code | High | 2 | 5.2 |
| 5.5 | Upload `kotlforge-report.json` as workflow artifact | High | 1 | 5.2 |

**Total: 11 story points**

---

## Epic 6 — Documentation & OSS Hygiene

**Module:** shared | **Milestone:** MVP 0.1

Ensure the project is approachable for new contributors and meets open-source standards.

| BLI | Title | Priority | Estimate |
|---|---|---|---|
| 6.1 | Add `CODE_OF_CONDUCT.md` | Medium | 1 |
| 6.2 | Add `docs/architecture.md` with module diagram | Medium | 2 |
| 6.3 | Add `docs/compatibility-matrix.md` | Medium | 2 |
| 6.4 | Add `docs/troubleshooting.md` | Medium | 2 |
| 6.5 | Update `CODEOWNERS` with real GitHub usernames | High | 1 |
| 6.6 | End-to-end demo script — doctor → inspect → build → APK → report | High | 2 |

**Total: 10 story points**

---

## Epic 8 — Wireless Device Deployment

**Module:** `deploy` | **Milestone:** MVP 0.2

Deploy a built APK directly to a physical Android device over WiFi using ADB wireless — no USB cable, no emulator required. The device must be on the same network as the build machine.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 8.1 | Discover ADB binary from `ANDROID_HOME` or PATH | Critical | 2 | 3.1 |
| 8.2 | Detect connected devices over ADB (USB and WiFi) | Critical | 3 | 8.1 |
| 8.3 | Pair device over WiFi using ADB pairing code (Android 11+) | Critical | 3 | 8.2 |
| 8.4 | Connect to device wirelessly (`adb connect <ip>:<port>`) | Critical | 2 | 8.3 |
| 8.5 | Install APK on selected device (`adb install`) | Critical | 2 | 8.4, 3.7 |
| 8.6 | Add `deploy` CLI command — `--device`, `--apk`, `--variant` | High | 3 | 8.5 |
| 8.7 | Auto-select device when only one is connected | High | 2 | 8.2 |
| 8.8 | Handle multiple connected devices with prompt | High | 2 | 8.2 |
| 8.9 | Persist paired device info for reuse across sessions | Medium | 2 | 8.4 |
| 8.10 | Unit tests with mocked ADB process runner | High | 3 | 8.5 |
| 8.11 | Clear error when no devices found or ADB not installed | High | 1 | 8.2 |
| 8.12 | Document WiFi pairing steps and known device quirks | Medium | 2 | 8.5 |

**Total: 27 story points**

---

## Epic 9 — Hot Reload and Incremental Rebuild

**Module:** `watch` | **Milestone:** MVP 0.2

Watch the Android project source tree for file changes and trigger incremental Gradle builds automatically. Optionally redeploy the updated APK to a connected device after each successful build — giving a fast build-deploy loop without manual reruns.

| BLI | Title | Priority | Estimate | Depends On |
|---|---|---|---|---|
| 9.1 | Watch source directories for file changes using Java WatchService | Critical | 3 | 1.6 |
| 9.2 | Debounce rapid file saves to avoid redundant rebuilds | Critical | 2 | 9.1 |
| 9.3 | Trigger incremental Gradle build on change (`assemble` with Gradle daemon) | Critical | 3 | 9.1, 3.2 |
| 9.4 | Report changed files and rebuild duration after each cycle | High | 2 | 9.3 |
| 9.5 | Auto-redeploy APK to device after successful rebuild | High | 3 | 9.3, 8.5 |
| 9.6 | Add `watch` CLI command — `--project`, `--variant`, `--deploy` | High | 3 | 9.3 |
| 9.7 | Graceful shutdown on Ctrl+C — stop Gradle daemon cleanly | High | 2 | 9.3 |
| 9.8 | Configurable watch paths and ignore patterns in `kotlforge.json` | Medium | 2 | 9.1, 1.8 |
| 9.9 | Unit tests with mock file system events | High | 3 | 9.1 |
| 9.10 | Document watch mode limitations (no Kotlin compiler daemon restart) | Medium | 1 | 9.3 |

**Total: 24 story points**

---

## Sprint plan

| Week | Focus | BLIs |
|---|---|---|
| 1 | Contracts agreed, modules compile, repo wired up | 7.1–7.5, 1.1–1.4, 2.1, 2.4, 3.1, 4.1, 5.1 |
| 2 | Skeleton CLI, doctor working, process runner tested | 1.5–1.6, 2.2–2.6, 3.2–3.4, 4.2, 4.4–4.5 |
| 3 | First end-to-end Gradle build + result | 1.7–1.8, 2.7–2.9, 3.5–3.9, 4.3, 4.6–4.7 |
| 4 | MVP hardening, APK validation, JSON report, CI green | 1.9–1.12, 2.10, 3.10–3.12, 4.8–4.11, 5.2–5.5, 6.5–6.6 |
| 5 | Project generator, more diagnostics, tests, docs | 2.11–2.12, 4.12–4.13, 6.1–6.4 |
| 6 | Release candidate, demo, retrospective | 3.13–3.14 + release tag `v0.1.0` |

---

## Dependency map

```
Epic 7 (contracts)
    │
    ├──► Epic 1 (core models)
    │         │
    │         ├──► Epic 3 (build engine) ──► Epic 5 (sample + CI)
    │         │         │                           ▲
    │         │         └──► Epic 8 (deploy) ───────┘
    │         │                   │
    │         │                   └──► Epic 9 (hot reload)
    │         │
    │         └──► Epic 4 (CLI + reporting) ─────────┘
    │
    └──► Epic 2 (diagnostics) ──► Epic 4 (doctor command)
```

> Start Epic 7. Everything else is blocked until contracts are merged.
