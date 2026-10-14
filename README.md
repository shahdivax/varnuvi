# Voice Agent (working name)

Android voice-dictation helper in the spirit of Wispr Flow: a small floating
bubble appears over text fields in apps you choose, and tapping it puts text
into the field. It is **not** a keyboard; you keep Gboard or whatever you use.

This phase is a UI shell plus the native Android layer. Tapping the bubble
inserts a **hardcoded sample sentence**. There is no speech recognition, no
agent, no network access, and no login yet.

The working name lives in `lib/config/app_config.dart` (`kAppName`) and
`android/app/src/main/res/values/strings.xml` (`app_name`).

## Bring your own API keys

The app ships with no keys, endpoints, or accounts. Future speech-to-text and
agent providers will read your own keys from a local, gitignored config file:

```sh
cp config.example.json local.config.json   # fill in your own values
flutter run --dart-define-from-file=local.config.json
```

`local.config.json` is in `.gitignore`. Never commit it. Run
`scripts/check_secrets.sh` before committing; it refuses staged local config
files and secret-looking strings.

Providers are swappable: `lib/providers/` defines `SpeechToTextProvider` and
`AgentProvider` interfaces. Only mock implementations exist today.

## Requirements

- Flutter SDK (Dart 3.5+), Android SDK, JDK 17
- Android device or emulator running Android 13+ (minSdk 33)

## How to run

```sh
flutter pub get
flutter run            # debug build on a connected device
flutter build apk      # release APK in build/app/outputs/flutter-apk/
flutter analyze && flutter test
```

The Gradle wrapper scripts/jar are not committed; the Flutter tool generates
them (and `android/local.properties`) on the first build.

## Enable the bubble on a device

1. Open the app, go to **Settings**, tap **Turn on in Android settings**.
2. In Android accessibility settings, open **Installed apps** (wording varies by
   device), pick **Voice Agent**, and turn it on. Android 13+ may first require
   "Allow restricted settings" from the app info page for sideloaded APKs.
3. Back in the app, open **Apps** and turn on the apps where you want the bubble.
   Every app is off by default.
4. Open one of those apps and tap a normal text field. The bubble appears; drag
   it anywhere (the spot is remembered). Tap it: it shows listening, then
   processing, then inserts the sample sentence.

Debug log: `adb logcat -s VoiceA11y` shows which package got the bubble and
which insertion path worked (`SET_TEXT`, `PASTE`, or `FAILED`). Field text is
never logged.

## Privacy

- The accessibility service checks only whether the focused field is editable
  and which app it belongs to.
- On a bubble tap it reads the field's current text in memory, only to merge the
  insertion at the cursor. Nothing on screen is logged, stored, or sent.
- Password and other sensitive fields are ignored entirely: no bubble, no reads.
- The release manifest has no INTERNET permission (the debug manifest has it
  only for Flutter hot reload).

## Folder structure

```
lib/
  bridge/        native_bridge.dart (the only MethodChannel on the Dart side)
  config/        app_config.dart (working name)
  mock/          sample sentence, mock history, languages, fake dictation timer
  models/        transcript, modes, mic state, installed app
  providers/     STT and agent interfaces + mock implementations
  screens/       home, apps, history (+ detail), settings
  settings/      in-memory app settings
  shell/         bottom navigation scaffold
  theme/         colors, light/dark themes, theme mode controller
  utils/         time formatting, copy to clipboard
  widgets/       mic button, chips, cards, tiles
android/app/src/main/kotlin/dev/voiceagent/app/
  MainActivity.kt
  bridge/        NativeBridge.kt (the only MethodChannel on the Kotlin side)
  accessibility/ service, focused-field checks, service status
  allowlist/     enabled packages in SharedPreferences
  apps/          launchable app list + icon encoding
  overlay/       bubble window, view, drag, position, states, fake timer
  insert/        text insertion (SET_TEXT, paste fallback), mock text
test/            unit tests
scripts/         check_secrets.sh
```

## Known limitations

- Nothing in this repo has been compiled or run on a device yet (see
  PROGRESS.md); expect small build fixes on first `flutter run`.
- Some apps block or ignore accessibility text insertion; the clipboard paste
  fallback may also fail there.
- Secure and sensitive fields are skipped by design.
- Custom-rendered text fields (games, Flutter or other canvas UIs, some web
  views) may not expose an editable node, so the bubble will not appear.
- The paste fallback overwrites the clipboard and does not restore it.
- Theme, default mode, and language are in memory only and reset on restart.
- History is mock data; nothing is saved.

## Intentionally not built in this phase

- Speech recognition and microphone permission
- The transformation agent (paraphrase, translate, bullets, summary) and its API
- Login, accounts, any backend or network call
- Snippets and custom dictionary
- A keyboard (InputMethodService); this app will never replace your keyboard
- iOS, web, and desktop targets

## License

MIT, see [LICENSE](LICENSE).
