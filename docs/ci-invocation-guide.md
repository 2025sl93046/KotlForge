# KotlForge CLI — CI Invocation Guide

This guide shows how to invoke KotlForge in Jenkins, GitLab CI, and Bitrise pipelines.

---

## Prerequisites

All CI environments require:
- **JDK 17+** — set `JAVA_HOME`
- **Android SDK** — set `ANDROID_HOME`
- **KotlForge installed** — build from source with `./gradlew :cli:installDist`

The CLI binary after install is at: `cli/build/install/cli/bin/cli`

---

## Jenkins

```groovy
pipeline {
    agent any

    environment {
        JAVA_HOME  = '/usr/lib/jvm/java-17-openjdk'
        ANDROID_HOME = '/opt/android-sdk'
    }

    stages {
        stage('Build KotlForge') {
            steps {
                sh './gradlew :cli:installDist -x test'
            }
        }

        stage('Check Toolchain') {
            steps {
                sh './cli/build/install/cli/bin/cli doctor --project .'
            }
        }

        stage('Build APK') {
            steps {
                sh './cli/build/install/cli/bin/cli build --project . --variant debug'
            }
        }

        stage('Archive Report') {
            steps {
                archiveArtifacts artifacts: 'kotlforge-report.json', allowEmptyArchive: true
            }
        }
    }

    post {
        failure {
            archiveArtifacts artifacts: 'kotlforge-build.log', allowEmptyArchive: true
        }
    }
}
```

---

## GitLab CI

```yaml
# .gitlab-ci.yml

variables:
  JAVA_HOME: /usr/lib/jvm/java-17-openjdk
  ANDROID_HOME: /opt/android-sdk

stages:
  - build
  - verify

install-kotlforge:
  stage: build
  script:
    - ./gradlew :cli:installDist -x test

doctor:
  stage: verify
  script:
    - ./cli/build/install/cli/bin/cli doctor --project .

build-apk:
  stage: verify
  script:
    - ./cli/build/install/cli/bin/cli build --project . --variant debug
  artifacts:
    paths:
      - kotlforge-report.json
    when: always
    expire_in: 7 days
```

---

## Bitrise

Add the following **Script** steps to your Bitrise workflow:

**Step 1 — Install KotlForge:**
```bash
./gradlew :cli:installDist -x test
```

**Step 2 — Check toolchain:**
```bash
./cli/build/install/cli/bin/cli doctor --project .
```

**Step 3 — Build APK:**
```bash
./cli/build/install/cli/bin/cli build \
  --project $BITRISE_SOURCE_DIR \
  --variant debug \
  --format json
```

**Step 4 — Deploy file path** (use Deploy to Bitrise.io step):
- File path: `$BITRISE_SOURCE_DIR/kotlforge-report.json`

**Required Bitrise environment variables:**

| Variable | Description |
|---|---|
| `JAVA_HOME` | Path to JDK 17+ installation |
| `ANDROID_HOME` | Path to Android SDK |
| `BITRISE_SOURCE_DIR` | Auto-set by Bitrise — root of your Android project |

---

## Exit Codes

| Code | Meaning | CI behaviour |
|---|---|---|
| `0` | Success | Pipeline continues |
| `1` | Build failed | Pipeline fails |
| `2` | Bad arguments | Pipeline fails — check your command |
| `3` | Environment error | Pipeline fails — check JDK/SDK setup |

Use `doctor` as an early gate step to catch environment issues before attempting a build.
