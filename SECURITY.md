# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| `main` (latest) | Yes |
| Older releases | No |

## Reporting a Vulnerability

If you discover a security vulnerability in KotlForge, **do not open a public GitHub issue.**

Report it privately by emailing the maintainer or opening a [GitHub Security Advisory](https://github.com/2025sl93046/KotlForge/security/advisories/new).

Include:
- A description of the vulnerability
- Steps to reproduce it
- The potential impact
- Any suggested fix (optional)

You will receive a response within **7 days**. If the vulnerability is confirmed, a fix will be prioritized and a patched release will be tagged. You will be credited in the release notes unless you request otherwise.

## Scope

KotlForge is a build orchestration CLI tool. The following are in scope:

- Command injection via unsanitized project paths or build arguments
- Unintended file writes outside the target project directory
- Credential or secret leakage via build logs, `kotlforge-report.json`, or diagnostic output
- ADB command injection via device serial or APK path inputs

The following are out of scope:

- Vulnerabilities in the target Android project being built (KotlForge does not own that code)
- Vulnerabilities in Gradle, ADB, or the Android SDK themselves
- Issues on unsupported or end-of-life versions

## Security Principles

KotlForge is designed to be non-invasive:

- It never modifies the target Android project
- It never stores credentials or signing keys
- Build logs (`kotlforge-build.log`) are written only on failure and contain only Gradle stdout/stderr
- `kotlforge-report.json` must never include secret values — this is enforced by the data contract (see `docs/CONTRACTS.md`)
- `JAVA_HOME` and `ANDROID_HOME` are forwarded to Gradle subprocesses but no other environment variables are passed through
