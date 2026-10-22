# Contributing

Keep changes focused and preserve the app's minimal UI. Reuse Android/Flutter
platform features before adding dependencies. Retain application IDs and stored
data compatibility unless a migration is part of the change.

Read [Architecture](docs/ARCHITECTURE.md) and [Development](docs/DEVELOPMENT.md).
Test meaningful behavior, including permission denial, explicit copy, cancellation,
request authentication, and error paths. Never add fake listening/transcripts,
silent clipboard writes, automatic repeated pastes, or logs of audio/tokens/text.

Before a contribution:

```sh
flutter analyze
flutter test
python3 scripts/check_chunking.py
python3 scripts/check_accessibility.py
python3 scripts/check_public_source.py
```

Run the backend checks described in Development when changing `server/`.
After staging, run `bash scripts/check_secrets.sh` and `git diff --cached --check`.
Review every staged file; these lightweight checks are not a full secret scanner.

Keep keystores, keys, local config, audio, private screenshots, diagnostics, and
build outputs out of commits. Use synthetic content for screenshots and tests.
PRs should explain resulting behavior, validation, and material limitations.
Report vulnerabilities as described in [Security](SECURITY.md).

Contributions use the existing [MIT license](LICENSE).
