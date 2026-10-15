# Contributing to Varnuvi

The repository is private during development. Contributions are coordinated
with the maintainer; please do not publish this checkout, build artifacts,
recordings, or private infrastructure details.

## Before changing code

Read the [development guide](docs/DEVELOPMENT.md) and relevant
[architecture boundary](docs/ARCHITECTURE.md). Keep changes focused. Reuse Flutter,
Android, and existing helpers before adding a dependency. Preserve application
IDs and stored-data compatibility unless a migration is part of the change.

Data-loss prevention, accessible controls, bounded background work, and truthful
UI states take priority over visual effects. Never introduce simulated listening,
fake transcripts, silent partial output, automatic repeated pastes, or logging
of keys, audio, transcripts, or field contents.

## Pull requests

Explain the user-visible problem, resulting behavior, and validation. Include
small screenshots for UI changes using synthetic content only. Note device/OS,
build mode, and reproduction steps for accessibility or recording changes.
Add a focused regression check for new behavior, especially chunk ordering,
retry deduplication, insertion safety, lifecycle cleanup, and polling budgets.

Run the commands in the development guide. Review `git diff --check` and run
`bash scripts/check_secrets.sh` after staging. This lightweight staged check is
not a full secret scanner; review every staged file yourself. Do not commit
keystores, credentials, local configuration, JVM dumps, personal screen captures,
or device recordings. Commit `pubspec.lock` for reproducible app dependencies.

Report security issues privately as described in [SECURITY.md](SECURITY.md).
Contributions use the existing [MIT license](LICENSE). Be respectful, concrete,
and open to revision in reviews.
