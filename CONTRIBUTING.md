# Contributor Guide

## Branching

- Branch from `main`: `feature/<short-name>` or `fix/<short-name>`
- One issue per branch

## Pull requests

- Link your PR to an issue: `Closes #<number>`
- Fill in the PR template
- CI must pass before merging
- At least one approval required

## Code style

- Kotlin only
- Detekt and ktlint enforced via `./gradlew check`

## Commit messages

Use imperative present tense: `Add Gradle process runner`, `Fix APK path detection`

---

## Local development setup

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or 21 | JDK 25 breaks Gradle 8.x — do not use |
| Android SDK | any recent | using commandline tools for sdk |

### Install JDK 17 

```bash
brew install --cask sapmachine-jdk17
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

### Install Android SDK (no Android Studio needed)

```bash
brew install android-commandlinetools
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Persist both env vars in your shell:

```bash
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 17)' >> ~/.zshrc
echo 'export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools' >> ~/.zshrc
source ~/.zshrc
```

---

## Building KotlForge

```bash
./gradlew build
```

## Running unit tests

```bash
./gradlew test
```

## Testing the CLI end-to-end

Build the CLI distribution first:

```bash
./gradlew :cli:installDist
```

Then point it at any existing Android project (do **not** modify the target project):

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./cli/build/install/cli/bin/cli build --project /path/to/android-project
```

> KotlForge is non-invasive — it never writes files into the target project.
> It forwards `JAVA_HOME` and `ANDROID_HOME` to the Gradle subprocess automatically.

### Quick test with the Google Sunflower sample

```bash
git clone https://github.com/android/sunflower /tmp/sunflower

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./cli/build/install/cli/bin/cli build --project /tmp/sunflower
```

Expected output:

```
Building debug variant at: /tmp/sunflower
Build successful
APK: /tmp/sunflower/app/build/outputs/apk/debug/app-debug.apk
```
