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

### 5. Home mic states
- `MicState` enum (idle, listening, processing) and `FakeDictation` (ValueNotifier + Timer): tap -> listening 3 s -> processing 1.2 s -> idle; tapping while listening skips ahead.
- `MicButton` shows a soft pulse ring while listening, a thin spinner while processing; status line under it is a screen-reader live region.

### 6. History
- Mock `Transcript` list (`lib/mock/mock_history.dart`), newest first; `HistoryTile` shows mode, relative time, two-line preview, copy button.
- `HistoryDetailScreen`: full timestamp, selectable text, tonal Copy button; clipboard via built-in `Clipboard`. Unit test for `lib/utils/time_format.dart`.

### 7. Settings
- Theme selector (`SegmentedButton`: light, dark, system) wired to `ThemeController`; default mode reuses `ModeChips`; translation language dropdown from `lib/mock/mock_languages.dart`.
- `AppSettings` holds default mode and language as `ValueNotifier`s; Account section is a placeholder card saying it comes later.

### 8. Allowlist storage (native)
- `AllowlistStore.kt`: enabled package names as a string set in SharedPreferences, empty by default (bubble off everywhere).
- One MethodChannel `dev.voiceagent.app/bridge`: `NativeBridge.kt` + `lib/bridge/native_bridge.dart` with `getInstalledApps` (name + package, sorted, own app excluded), `getEnabledPackages`, `setPackageEnabled`.

### 9. Apps screen
- Bridge now sends each app's own launcher icon, rendered natively to a 96 px PNG by `AppIconEncoder.kt`; no bundled brand logos.
- `AppsScreen`: search (name or package), one `AppToggleTile` per app with a switch wired to `setPackageEnabled`; loading, error and no-match states.

### 10. Accessibility service skeleton
- `VoiceAccessibilityService.kt` declared in the manifest with `res/xml/accessibility_service_config.xml` (window state + view focused events, `canRetrieveWindowContent`, `flagDefault`).
- Logs only the package name when an editable, non-password field is focused; `FocusedField.kt` checks editable/password flags and input type, never text.

### 11. Setup flow
- `AccessibilitySetupCard` at the top of Settings: live on/off status, plain-language "what it does / does not do", button opening Android accessibility settings; rechecks on app resume.
- Bridge: `isAccessibilityServiceEnabled`, `openAccessibilitySettings` via `ServiceStatus.kt`.

### 12. Bubble overlay
- `overlay/BubbleOverlay.kt`: 52 dp round bubble in a `TYPE_ACCESSIBILITY_OVERLAY` window (no overlay permission), `FLAG_NOT_FOCUSABLE` so it never steals focus; `BubbleTouchListener` drags it, `BubblePositionStore` remembers the drop spot.
- Service shows it only when the active window belongs to an enabled package and its input-focused node is editable and not a password field; hides otherwise. Tap only logs for now (insertion is step 13).

### 13. Mock insertion
- Bubble tap inserts `MockText.SAMPLE_SENTENCE` via `insert/TextInserter.kt`: `ACTION_SET_TEXT` merged at the cursor (cursor moved after it), fallback clipboard + `ACTION_PASTE`.
- Service re-checks the focused field (enabled app, editable, not password) at tap time and logs only `Insert path=SET_TEXT|PASTE|FAILED` plus the package.

### 14. Bubble states
- `BubbleDictation.kt` (Handler fake timer, same 3 s / 1.2 s as Home): tap -> listening -> processing -> inserts the sample sentence -> idle; tapping while listening skips ahead.
- `BubbleStateRenderer.kt`: listening = deeper tone + gentle shrink pulse, processing = dots glyph + soft fade; content description per state. Focus loss cancels without inserting.

### 14b. Provider interfaces
- `lib/providers/`: abstract `SpeechToTextProvider` and `AgentProvider` (Dart 3 `interface class`), each with a mock implementation only; no real providers.
- Mocks return the sample sentence / the input unchanged; nothing is wired to real audio or network.

### 15. Polish
- History list padding aligned to the 24 px side margin used on every other tab; long setup-card button line wrapped.
- Mic button exposes one merged semantics node (label + tap) instead of a duplicate inner button; Apps loading spinner has a screen-reader label.

### 16. README
- `README.md`: how to run, enabling the service on a device, privacy, folder structure, known limitations, not-built list, bring-your-own-API-keys section.
- Phase complete. Nothing compiled or device-tested (no Flutter/Android SDK on the build machine).

### Design consistency pass
- Polish 1: `lib/theme/app_spacing.dart` holds the one page padding (24 px sides); every screen and the app tile use it. Settings top padding is now 16 like the other tabs.

### Polish 2. Theme shapes
- `app_theme.dart` now sets filled button, chip, segmented button and input shapes to the shared 16 px radius with the theme border colour.
- Apps search field no longer overrides its border; it uses the theme input style.

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
- Listening state uses a pulse ring, not a waveform (simpler, no fake audio levels needed).
- Timestamps formatted by hand (`lib/utils/time_format.dart`), no `intl` package. History is in-memory mock only; nothing is stored.
- Settings (default mode, translation language) are in-memory only, same as theme; persistence can come with the native prefs bridge later.
- Package visibility via a `<queries>` MAIN/LAUNCHER block, not QUERY_ALL_PACKAGES (narrower, Play-policy friendly).
- App list is loaded on a plain background thread and posted back to the main looper (no coroutines dependency).
- App icons were left out of step 8 and added in step 9, where the Apps screen needs them.
- Icons cross the channel as PNG bytes (Kotlin ByteArray to Dart Uint8List), loaded together with the app list.
- App toggles are optimistic and revert with a snackbar if the native save fails.
- Event type `typeAllWindowStateChanged` from the brief is not a real constant; used `typeWindowStateChanged|typeViewFocused`.
- Accessibility service is `exported="true"` guarded by `BIND_ACCESSIBILITY_SERVICE` (standard pattern; only the system can bind).
- Password detection uses `isPassword` plus password input-type variations (text, visible, web, number).
- Setup card lives in Settings (Home keeps its single primary action). Service status read from `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (no extra permission).
- Bubble visibility is decided from `rootInActiveWindow` (package + input focus), not the event package, so the overlay window events never toggle it; our own app never shows it.
- Bubble stays while the field keeps input focus even if the keyboard is dismissed (no extra, noisier event types subscribed).
- Bubble colors in `res/values(-night)/colors.xml` mirror `app_colors.dart`; mic glyph is a vector drawable; default spot is the right edge at 40% height.
- Merging at the cursor needs the field's current text; it is read in memory only at tap time, never logged or stored. Service description reworded from "never reads" to "never stores or sends" to stay accurate.
- Hint text is treated as an empty field; missing cursor info appends at the end.
- Paste fallback overwrites the clipboard and does not restore it (background apps cannot read the clipboard on Android 10+).
- Bubble pulse only shrinks (1 -> 0.9), so it never clips inside its fixed-size overlay window. Animations follow the system animator scale.
- If the field loses focus mid-cycle the cycle is cancelled and nothing is inserted; insertion still re-checks eligibility at the end.
- One radius (16) for cards, buttons, chips, segmented control and inputs; selected chip/segment uses the accent container tint.
