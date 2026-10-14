# Progress

> Toolchain note: Flutter, Dart, Java and the Android SDK are NOT installed on the
> build machine. All files are hand-written. Nothing has been compiled, analyzed,
> formatted by tooling, or tested on a device yet.

## Steps

### 1. Skeleton
- pubspec, blank themed scaffold, Android-only Gradle (Kotlin DSL), minSdk 33, no iOS/web/desktop.
- Config layer: gitignored `local.config.json`, committed `config.example.json`; MIT license; `scripts/check_secrets.sh` run before each commit.

### 2. Theme
- Light/dark `ColorScheme`s built explicitly (no seed) from `lib/theme/app_colors.dart`; dusty slate blue accent, no pure white/black.
- `ThemeController` (ValueNotifier) switches light/dark/system; UI selector comes in Settings (step 7).

### 3. App shell
- `lib/shell/app_shell.dart`: M3 `NavigationBar` with Home, Apps, History, Settings; `IndexedStack` keeps tab state.
- Each tab is a tiny placeholder screen in `lib/screens/`, sharing `lib/widgets/placeholder_view.dart`.

### 4. Home
- Large central `MicButton` (idle only, no-op tap), `ModeChips` (Raw, Professional, Translate, Bullets, Summary), `TranscriptCard` with mock text.
- `TranscriptMode` enum in `lib/models/`, sample sentence in `lib/mock/mock_data.dart`.

## Decisions
- Package/applicationId `dev.voiceagent.app`; working name lives in `lib/config/app_config.dart` (plus Android `strings.xml` for the launcher label).
- License: MIT (simplest permissive choice).
- Config format: JSON passed with `flutter run --dart-define-from-file=local.config.json` (built into Flutter, no package needed).
- No `gradlew`/wrapper jar committed (binary); the Flutter tool regenerates the wrapper on first build. Wrapper pins Gradle 8.10.2, AGP 8.7.3, Kotlin 2.1.0.
- INTERNET permission only in `src/debug` manifest (needed by Flutter tool for hot reload); release/main manifest has no network permission.
- Launcher icon: system default icon (no binary assets in this phase).
- Theme mode is in-memory only (no storage package); resets to system on restart.
- No `cardTheme` in ThemeData to stay compatible across Flutter versions (CardTheme vs CardThemeData rename); cards get styled per widget.
- Navigation: plain `setState` + `IndexedStack`, no router package.
- Home mode chip selection is local UI state only; the card label follows the selected chip.
