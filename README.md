# KotlForge

KotlForge is a Kotlin-powered command-line tool that builds existing Android projects. Point it at any Android project that has a Gradle Wrapper and it will invoke the build, locate the APK, and report the result.

## Features

- Detects your JDK and Android SDK configuration (`doctor`)
- Invokes an Android project's Gradle Wrapper to produce an APK (`build`)
- Reports build output as structured JSON
- Works with any Android project that uses a Gradle Wrapper

## Requirements

| Requirement | Version |
|---|---|
| JDK | 17 or higher |
| Gradle Wrapper | present in the target Android project |
| Android SDK | required for building Android projects |

## Getting started

**1. Clone the repository**

```bash
git clone https://github.com/2025sl93046/KotlForge.git
cd KotlForge
```

**2. Run setup (first time only)**

```bash
./setup.sh
```

This pins the correct JDK, validates the Gradle Wrapper, and persists the configuration to your shell.

**3. Build the project**

```bash
./build.sh
```

**4. Run the CLI**

```bash
# Unpack the distribution
unzip -o cli/build/distributions/cli.zip -d /tmp/kotlforge-dist

# Check your toolchain
/tmp/kotlforge-dist/cli/bin/cli doctor

# Build an Android project
/tmp/kotlforge-dist/cli/bin/cli build --project /path/to/android-app
```

## CLI commands

### `doctor`

Checks that your machine has the required tools configured.

```
$ cli doctor

KotlForge Doctor
  JDK:         /path/to/jdk
  Android SDK: /path/to/android-sdk
  Status: OK
```

### `build`

Builds an Android project using its own Gradle Wrapper.

```
$ cli build --project /path/to/android-app --variant debug

Building debug variant at: /path/to/android-app
Build successful
APK: /path/to/android-app/app/build/outputs/apk/debug/app-debug.apk
```

| Flag | Default | Description |
|---|---|---|
| `--project`, `-p` | `.` (current directory) | Path to the Android project |
| `--variant`, `-v` | `debug` | Build variant (`debug` or `release`) |

## Project structure

```
KotlForge/
├── core/               BuildRequest and BuildResult data models
├── diagnostics/        JDK, Android SDK, and Gradle toolchain detection
├── build-engine/       Gradle Wrapper process runner and APK locator
├── cli/                CLI entry point and commands
├── samples/
│   └── android-app/    Sample Android project used for integration testing
├── .github/
│   ├── workflows/      CI, integration, and release pipelines
│   └── ISSUE_TEMPLATE/ Bug report and feature request templates
├── setup.sh            One-time environment setup
└── build.sh            Project build script
```

## Development

```bash
./gradlew clean check   # compile all modules, run tests, run static analysis
./gradlew build         # build all modules and produce distributions
./gradlew :cli:run --args="doctor"                            # run CLI directly
./gradlew :cli:run --args="build --project /path/to/app"     # run build command
```

## Module overview

| Module | Responsibility |
|---|---|
| `core` | Shared data models: `BuildRequest`, `BuildResult` |
| `diagnostics` | Detects JDK and Android SDK, validates Gradle/AGP compatibility |
| `build-engine` | Runs the target project's Gradle Wrapper, locates the APK output |
| `cli` | Clikt-based CLI: `doctor` and `build` subcommands |

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

| Pipeline | Trigger | Purpose |
|---|---|---|
| `ci.yml` | Every PR and push to `main` | Compile, test, static analysis |
| `integration.yml` | PRs touching build-related modules | Build a real Android APK end to end |
| `release.yml` | Tags matching `v*` | Produce versioned distributions |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for branching conventions, PR requirements, and code style rules.

## License

MIT — see [LICENSE](LICENSE).
