# Security Policy

## Supported Versions

KotlForge is currently in active development. Only the latest release on `main` receives security fixes.

| Version | Supported |
|---|---|
| `main` (latest) | ✅ |
| Older releases | ❌ |

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

Use GitHub's private vulnerability reporting instead:

1. Go to the [Security tab](https://github.com/2025sl93046/KotlForge/security) of this repository
2. Click **"Report a vulnerability"**
3. Fill in the details and submit

You will receive an acknowledgement within **48 hours** and a resolution plan within **7 days**.

---

## What to Include in Your Report

A good report helps us reproduce and fix the issue faster. Please include:

- A clear description of the vulnerability
- The component affected (`cli`, `build-engine`, `diagnostics`, `deploy`, `watch`, `core`)
- Steps to reproduce the issue
- The impact — what an attacker could do
- If possible, a proof-of-concept or sample project that triggers the issue

---

## Scope

KotlForge is a local CLI tool that wraps Android Gradle builds. The following are **in scope**:

| Area | Example |
|---|---|
| Path traversal | Accessing files outside the target project directory |
| Command injection | Unsanitised project path or variant name passed to a shell |
| Credential leakage | Environment variables or secrets appearing in logs or report JSON |
| Malicious project handling | A crafted `build.gradle.kts` causing unintended behaviour in KotlForge itself |
| ADB communication | Insecure handling of device serial or pairing codes |

The following are **out of scope**:

- Vulnerabilities in the user's own Android project or Gradle wrapper
- Issues requiring physical access to the developer's machine
- Bugs in third-party dependencies (report these upstream: Clikt, kotlinx.serialization, ADB)

---

## Security Design Notes

KotlForge follows these practices to reduce attack surface:

- **Credential sanitisation** — the `doctor` command redacts values that look like credentials from diagnostic output (see `ToolchainDetector.sanitize()`)
- **No network calls** — KotlForge never makes outbound HTTP requests; all network activity is delegated to the target project's Gradle wrapper
- **Non-invasive** — KotlForge never writes files into the target Android project
- **No shell expansion** — project paths and variant names are passed as `ProcessBuilder` arguments, not concatenated into shell strings

---

## Disclosure Policy

Once a fix is merged and released:

1. The reporter is credited in the release notes (unless they prefer to remain anonymous)
2. A GitHub Security Advisory is published
3. The fix is tagged in a patch release

We ask reporters to keep the vulnerability private until a fix is available.
