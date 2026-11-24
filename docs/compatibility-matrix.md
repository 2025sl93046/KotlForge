# KotlForge Toolchain Compatibility Matrix

This document records the toolchain combinations that have been validated by the
`ToolchainDetector` test suite (Epic 2 — BLIs 2.3–2.9).

---

## Supported combinations

| JDK | Kotlin | Gradle | AGP    | Status |
|-----|--------|--------|--------|--------|
| 17  | 2.x    | 8.11   | 8.1.x  | Tested |
| 17  | 2.x    | 8.11   | 8.2.x  | Tested |
| 21  | 2.x    | 8.11   | 8.1.x  | Tested |
| 21  | 2.x    | 8.11   | 8.2.x  | Tested |

> **Tested** means the `ToolchainDetector` correctly detects the version,
> produces an `OK` diagnostic, and all related unit tests pass on CI.

---

## KotlForge build toolchain (this project)

| Tool    | Version |
|---------|---------|
| Kotlin  | 2.4.21  |
| Gradle  | 8.11    |
| JUnit 5 | 5.11.3  |
| MockK   | 1.13.13 |
| Detekt  | 1.23.8  |

---

## Known incompatibilities

| Combination              | Problem                                      | Reference        |
|--------------------------|----------------------------------------------|------------------|
| JDK 25 + Gradle 8.x      | Gradle 8.x does not support JDK 25           | instructions.pdf |
| AGP < 7.0 + Gradle 8.x   | Minimum AGP for Gradle 8 is 7.0              | AGP release notes |

---

## Android SDK packages

The detector requires at least one entry in each of the following directories
under `ANDROID_HOME`:

| Directory      | Minimum requirement         | Install command                        |
|----------------|-----------------------------|----------------------------------------|
| `platforms/`   | One `android-XX` subfolder  | `sdkmanager "platforms;android-35"`    |
| `build-tools/` | One version subfolder       | `sdkmanager "build-tools;35.0.0"`      |

---

## How versions are detected

| Field          | Source (priority order)                                              |
|----------------|----------------------------------------------------------------------|
| `jdkPath`      | `JAVA_HOME` env var                                                  |
| `androidSdkPath` | `ANDROID_HOME` → `ANDROID_SDK_ROOT` → standard platform paths     |
| `gradleVersion`  | `gradle/wrapper/gradle-wrapper.properties` (`distributionUrl`)     |
| `agpVersion`     | `gradle/libs.versions.toml` (`agp = "..."`) → `build.gradle.kts`  |
