# Code of Conduct

## Our Pledge

We as contributors and maintainers pledge to make participation in KotlForge a respectful and collaborative experience for everyone, regardless of background, experience level, or identity.

## Our Standards

**Expected behavior:**

- Write clear, well-scoped pull requests with descriptions that explain the why, not just the what
- Review others' code constructively — comment on the code, not the person
- Raise concerns early via GitHub Issues or Discussions before writing code
- Respect agreed contracts (see `docs/CONTRACTS.md`) — propose changes through the documented process, not unilateral edits
- Keep secrets out of commits — no credentials, signing keys, or personal tokens in any file
- Accept that your PR may be reworked or declined, and engage with feedback professionally

**Unacceptable behavior:**

- Merging to `main` without a passing CI run or the required approval
- Bypassing branch protection with `--no-verify` or force-push without team agreement
- Committing generated files, build artifacts, or IDE configs that belong in `.gitignore`
- Closing or dismissing another contributor's issue or PR without explanation
- Harassment, personal attacks, or exclusionary language in any project space (issues, PRs, discussions, commits)

## Scope

This Code of Conduct applies to all project spaces: the GitHub repository, issues, pull requests, discussions, and any communication directly related to KotlForge.

## Enforcement

Violations can be reported by opening a private GitHub issue or contacting the project maintainer directly. All reports will be reviewed and investigated. Maintainers are obligated to maintain confidentiality with respect to the reporter.

Consequences for violations range from a written warning to permanent removal from the project, depending on severity and recurrence.

## Contributor Responsibilities

Every contributor is responsible for:

- Running `./gradlew check` locally before opening a PR (compiles, tests, detekt)
- Not pushing `docs/instructions.md` or `docs/instructions.pdf` to the remote
- Using the BITS GitHub account (`2025sl93046@wilp.bits-pilani.ac.in`) for all commits to this repository
- Linking every PR to an issue with `Closes #<number>`

## Attribution

This Code of Conduct is adapted from the [Contributor Covenant](https://www.contributor-covenant.org), version 2.1.
