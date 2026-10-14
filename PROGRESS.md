# Progress

> Toolchain note: Flutter, Dart, Java and the Android SDK are NOT installed on the
> build machine. All files are hand-written. Nothing has been compiled, analyzed,
> formatted by tooling, or tested on a device yet.

## Steps

### 1. Skeleton
- pubspec, blank themed scaffold, Android-only Gradle (Kotlin DSL), minSdk 33, no iOS/web/desktop.
- Config layer: gitignored `local.config.json`, committed `config.example.json`; MIT license; `scripts/check_secrets.sh` run before each commit.

## Decisions
- Package/applicationId `dev.voiceagent.app`; working name lives in `lib/config/app_config.dart` (plus Android `strings.xml` for the launcher label).
- License: MIT (simplest permissive choice).
- Config format: JSON passed with `flutter run --dart-define-from-file=local.config.json` (built into Flutter, no package needed).
- No `gradlew`/wrapper jar committed (binary); the Flutter tool regenerates the wrapper on first build. Wrapper pins Gradle 8.10.2, AGP 8.7.3, Kotlin 2.1.0.
- INTERNET permission only in `src/debug` manifest (needed by Flutter tool for hot reload); release/main manifest has no network permission.
- Launcher icon: system default icon (no binary assets in this phase).
