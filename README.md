# KotlForge

KotlForge is a Kotlin-powered command-line tool that builds, deploys, and watches Android projects. Point it at any Android project that has a Gradle Wrapper and it will invoke the build, locate the APK, deploy it to a connected device, and watch for changes to rebuild automatically.

## Features

- Detects your JDK and Android SDK configuration (`doctor`)
- Builds any Android project using its own Gradle Wrapper (`build`)
- Deploys APKs to connected devices over USB or WiFi (`deploy`)
- Watches source files and rebuilds automatically on change (`watch`)
- Reports build output as structured JSON (`--format json`)
- Works with any Android project — never modifies the target project

## Requirements

| Requirement | Version |
|---|---|
| JDK | 17 or higher |
| Gradle Wrapper | present in the target Android project |
| Android SDK | required for building Android projects |
| ADB | required for `deploy` and `watch --auto-deploy` |

## Getting started

**1. Clone the repository**

```bash
git clone https://github.com/2025sl93046/KotlForge.git
cd KotlForge
```

**2. Build and install the CLI**

```bash
./gradlew :cli:installDist
```

**3. Run the CLI**

```bash
# Check your toolchain
./cli/build/install/cli/bin/cli doctor

# Build an Android project
./cli/build/install/cli/bin/cli build --project /path/to/android-app

# Deploy APK to a connected device
./cli/build/install/cli/bin/cli deploy --project /path/to/android-app

# Watch for changes and rebuild automatically
./cli/build/install/cli/bin/cli watch --project /path/to/android-app
```

## CLI commands

### `doctor`

Checks that your machine has the required tools configured.

```
$ cli doctor

KotlForge Doctor
  JDK:         /path/to/jdk  [OK]
  Android SDK: /path/to/sdk  [OK]
```

### `build`

Builds an Android project using its own Gradle Wrapper.

```
$ cli build --project /path/to/android-app --variant debug

Building debug variant at: /path/to/android-app
Build successful (12345ms)
APK: /path/to/android-app/app/build/outputs/apk/debug/app-debug.apk (15360KB)
```

| Flag | Default | Description |
|---|---|---|
| `--project`, `-p` | `.` | Path to the Android project |
| `--variant`, `-v` | `debug` | Build variant (`debug` or `release`) |
| `--format` | `text` | Output format (`text` or `json`) |

### `deploy`

Builds and deploys an APK to a connected Android device over USB or WiFi.

```
$ cli deploy --project /path/to/android-app
```

**WiFi pairing (Android 11+):**

```bash
# Step 1 — pair with the device (one time only)
cli deploy --pair 192.168.1.5:37853 --code 123456

# Step 2 — connect to the device
cli deploy --connect 192.168.1.5:5555

# Step 3 — deploy
cli deploy --project /path/to/android-app
```

| Flag | Description |
|---|---|
| `--project`, `-p` | Path to the Android project |
| `--variant`, `-v` | Build variant (default: `debug`) |
| `--device`, `-d` | Target device serial (auto-selected if only one connected) |
| `--pair` | WiFi pairing address (`ip:port`) |
| `--code` | Pairing code for WiFi pairing |
| `--connect` | WiFi connection address (`ip:port`) |

### `watch`

Watches source files and rebuilds (and optionally redeploys) automatically on every change.

```
$ cli watch --project /path/to/android-app --auto-deploy

[watch] Watching 1 path(s). Press Ctrl+C to stop.
[watch] Cycle 1 — 1 file(s) changed: Color.kt
[watch] Building debug...
[watch] Build succeeded in 12345ms
[watch]   APK: .../app-debug.apk
[watch] Deploying ...
[watch] Deployed to 192.168.1.5:5555
```

| Flag | Description |
|---|---|
| `--project`, `-p` | Path to the Android project |
| `--variant`, `-v` | Build variant (default: `debug`) |
| `--deploy`, `-d` | Deploy to this device serial after each successful build |
| `--auto-deploy` | Deploy to auto-selected device after each successful build |

## Configuration

Optionally add a `kotlforge.json` file to your Android project root to customise behaviour:

```json
{
  "defaultVariant": "debug",
  "defaultTasks": [],
  "jdkPath": null,
  "watchPaths": ["src"],
  "watchIgnorePatterns": ["**/*.class", "**/build/**", "**/.gradle/**", "**/.git/**", "**/kotlforge-build.log"]
}
```

## Architecture

```
+------------------------------------------------------------------+
|                        User / Terminal                           |
+------------------------------------------------------------------+
                               |
                               v
+------------------------------------------------------------------+
|                         cli module                               |
|   DoctorCommand   BuildCommand   DeployCommand   WatchCommand    |
+----+----------+--------+---------------+---------------+---------+
     |          |        |               |               |
     v          v        v               v               v
+----------+ +--------+ +-----------+ +--------+ +------------+
|diagnostics| |  core  | |build-engine| | deploy | |   watch    |
|ToolchainDe| |models  | |GradleRunner| |AdbLocator|WatchEngine|
|tector     | |        | |            | |Deployer| |FileWatcher |
+-----------+ +--------+ +------------+ +--------+ +------------+
                               |
                               v
+------------------------------------------------------------------+
|               Target Android Project (external)                  |
|   ./gradlew assembleDebug                                        |
|   app/build/outputs/apk/debug/app-debug.apk                     |
+------------------------------------------------------------------+
```

## Project structure

```
KotlForge/
├── core/               Shared models: BuildRequest, BuildResult, KotlForgeConfig
├── diagnostics/        JDK, Android SDK, and Gradle toolchain detection
├── build-engine/       Gradle Wrapper process runner and APK locator
├── deploy/             ADB device detection, WiFi pairing, APK installation
├── watch/              File system watcher, debounce, incremental rebuild loop
├── cli/                Clikt-based CLI: doctor, build, deploy, watch
├── detekt.yml          Static analysis configuration
├── .github/
│   ├── workflows/      CI, integration (matrix AGP), and release pipelines
│   └── ISSUE_TEMPLATE/ Bug report and feature request templates
├── setup.sh            One-time environment setup
└── build.sh            Project build script
```

## Module overview

| Module | Responsibility |
|---|---|
| `core` | Shared data models: `BuildRequest`, `BuildResult`, `KotlForgeConfig` |
| `diagnostics` | Detects JDK and Android SDK, validates toolchain |
| `build-engine` | Runs the target project's Gradle Wrapper, locates APK/AAB output |
| `deploy` | ADB device detection, WiFi pairing/connect, APK installation |
| `watch` | File system monitoring, debounce, incremental rebuild loop |
| `cli` | Clikt-based CLI: `doctor`, `build`, `deploy`, `watch` subcommands |

## Development

```bash
./gradlew clean check        # compile, run tests, run detekt
./gradlew build              # build all modules
./gradlew detekt             # static analysis only
./gradlew :cli:installDist   # build and install CLI locally
```

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.1 (JVM) |
| Build system | Gradle 8.11 with Kotlin DSL |
| CLI framework | Clikt 4.x |
| Serialization | kotlinx.serialization |
| Logging | kotlin-logging + SLF4J + Logback |
| Testing | JUnit 5 + MockK |
| Static analysis | Detekt |
| CI/CD | GitHub Actions |

## CI/CD

| Pipeline | Trigger | Jobs |
|---|---|---|
| `ci.yml` | Every PR and push to `main` | Unit Tests, Static Analysis, Build (parallel) |
| `integration.yml` | PRs touching build-related modules | Matrix APK build across AGP 8.1, 8.3, 8.6 |
| `release.yml` | Tags matching `v*` | Produce versioned distributions |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching conventions, PR requirements, and code style rules.

## License

MIT — see [LICENSE](LICENSE).
