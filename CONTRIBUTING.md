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
