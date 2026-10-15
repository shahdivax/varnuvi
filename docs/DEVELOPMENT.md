# Development

## Toolchain

The tested baseline is Flutter 3.47.7 / Dart 3.13.5, JDK 21, Android API 33+
minimum, Gradle 8.14.3, Android Gradle Plugin 8.11.1, and Kotlin 2.2.20. SDK compile
and target versions follow Flutter. Install the SDK components requested by the
build, then accept Android's licenses using your SDK's supported CLI.

```sh
flutter doctor -v
flutter config --android-sdk /path/to/Android/Sdk
flutter config --jdk-dir=/path/to/jdk-21
flutter pub get
flutter run -d <device-id>
```

Do not run Flutter as root. On Linux, a device with "no permissions" requires
USB/udev setup and USB debugging authorization; rerun `flutter devices` before
troubleshooting Gradle. Java 25 is not compatible with this project's Gradle.

`android/local.properties` is local and ignored. The Flutter tool generates the
Gradle wrapper launcher/JAR on first build. `android/gradle.properties` contains
low worker counts and JVM compilation flags because native JVM crashes were
observed on the original development workstation. These settings affect build
time, not application performance. Future toolchain support warnings are known;
upgrade Gradle, AGP, Kotlin, and Java as a tested set.

## Checks

```sh
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test
flutter build apk --debug
python3 scripts/check_chunking.py
git diff --check
bash scripts/check_secrets.sh  # staged changes only
```

Native checks compile pure Kotlin using the dependencies cached by the Android
build. Set `JAVA_HOME` to JDK 21 if another Java is on PATH. The check covers WAV
headers and exact sample continuity, the full 30-minute cap, cancellation,
response order, retry deduplication, Unicode cursor merging, AI on/off gating, and
bubble recovery timing. Flutter tests cover UI state, persistence calls, model
search, navigation, app grouping, hidden status polling, and theme/layout goldens.

CI is intentionally disabled. `templates/github-actions.yml.example` is an
inactive reference only; it does not run on pushes or pull requests. If CI is
added later, review its action versions, permissions, and clean-runner behavior.
Local automated checks never call the private transcription server or a paid model.
Update a golden intentionally with `flutter test --update-goldens`, inspect the
resulting image, and explain the visual change in the PR.

## Private transcription integration

The endpoint lives in `speech/TranscriptionApi.kt`, and the development HTTP
exception lives in `res/xml/network_security_config.xml`. Requests contain
`audio` (WAV), `language` (auto or saved spoken language), and `polish=true`.
Only a nonempty string `text` response is accepted. Both recording entry points
share this implementation. Keep current private infrastructure references in
this private checkout until publication preparation is explicitly scheduled.
`config.example.json` is legacy prototype configuration and is not read by the
current app; do not put credentials there.

## Device regression pass

Use synthetic text/audio. Do not publish screenshots of real notes or messages.

1. **Permissions:** fresh microphone denial/approval, accessibility off/on,
   service disconnection/reconnection, and all apps disabled.
2. **Bubble:** Home → enabled app → Home → enabled app; unlocked/screen-off
   transitions; move the system shortcut; keyboard open/closed; full system
   screens; rotation; tap shortcut to open Varnuvi directly.
3. **Insertion:** empty/filled field, cursor in middle, selected and reversed
   ranges, Unicode, password rejection, changed field/content/app during
   recording, and insertion failure recovering through History.
4. **Recording:** <30 seconds, several boundaries, repeated Submit, cancellation,
   permission revocation, disconnect/timeouts, failed-only retry, and leaving the
   recording screen. A real 30-minute soak is separate from the synthetic cap test.
5. **Writing:** AI off with saved preferences (no model call), AI on with all
   None and a spoken bullet-point request, spoken language only, translation,
   summary, tone/style, invalid key, no credits, output/context limits, and
   original-text recovery, persisted on/off choice, and key/model changes requiring
   another test. Authenticate using the app UI; do not paste keys into
   test fixtures or logs.
6. **Persistence:** restart, upgrade over existing install, clear history, remove
   key, and retained appearance/allowlist/defaults.

Use profile/release builds for resource measurements; debug builds include
hot-reload/JIT overhead. See [Performance](PERFORMANCE.md).
