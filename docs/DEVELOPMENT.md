# Development

Use Flutter/Dart, JDK 21 and an Android SDK with the components requested by
Gradle. The app requires Android 13/API 33+. Keep the checked-in Gradle, AGP and
Kotlin versions coordinated; known future-support warnings do not require an
unrelated upgrade. Do not run Flutter as root.

```sh
flutter doctor -v
flutter pub get
flutter analyze
flutter test
dart format --output=none --set-exit-if-changed lib test
flutter build apk --debug
python3 scripts/check_chunking.py
python3 scripts/check_accessibility.py
python3 scripts/check_public_source.py
git diff --check
```

Native checks use the cached Kotlin compiler from an Android build and JDK
selected by JAVA_HOME. They cover chunk boundaries/order/cancellation, AI gating,
and endpoint transport validation. UI tests cover setup, saved state, copy,
errors, navigation, and both themes. Goldens intentionally use Flutter's test
font. Update them only for deliberate layout changes and inspect screenshots on
an actual device as well.

## Backend checks

See [Self-hosting](SELF_HOSTING.md) for runtime setup. Tests replace inference
with a synthetic function; they never download a model, transcribe real speech,
or contact a deployed endpoint.

```sh
cd server
python3 -m venv .venv
. .venv/bin/activate
python -m pip install -r requirements-test.txt
python -m unittest discover -s . -p 'test_*.py' -v
```

They check auth-before-upload, health, valid multipart responses, invalid WAV,
size/duration/language rejection, overload, and temporary-file cleanup on errors.
An actual WAV smoke test is separately required to validate real inference and
hardware performance. PC/EC2 examples are reference deployment instructions,
not evidence that a cloud instance has been provisioned or load-tested.

## Device regression

Use synthetic text/audio and a server you operate. Never publish screenshots of
real notes, conversations, keys, or server addresses.

1. Fresh install: no server configured; starting dictation must explain setup
   without capturing or sending audio.
2. Settings: invalid URL, placeholder, HTTP public URL, wrong token, unreachable
   server, HTTPS success, replacement token, removed connection.
3. Capture: short recording, chunk boundaries, repeated Done, cancellation,
   network failure/retry, leaving the recording screen, microphone denial.
4. Bubble: display permission, show/hide, drag, lock/unlock, notification Hide,
   cancel/return, edited Unicode text, explicit copy, manual paste.
5. Writing: AI off, AI on after test, invalid key/model, changed connection,
   original-text recovery. Use your own key through the UI, never in fixtures.
6. Persistence: upgrade over a matching release signature, History, appearance,
   writing defaults, encrypted saved connection, removal/clear flows.
7. Banking compatibility: test on the target device. Some apps also restrict
   overlays, VPNs, debugging, or device integrity; do not promise universal support.

The native checks include an authenticated loopback HTTP round-trip and encrypted
settings checks. The result fixture uses synthetic text, with no microphone,
external server, or real clipboard writes:

```sh
cd android
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
cd ..
adb install -r build/app/outputs/flutter-apk/app-debug.apk
adb install -r build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w dev.voiceagent.app.test/dev.voiceagent.app.overlay.OverlayInstrumentation
```

Use an unlocked device. Synthetic light/dark result screenshots save under the
app's external files directory. The fixture is unexported and omitted from
release builds.

After staging, run `bash scripts/check_secrets.sh` and `git diff --cached --check`.
CI is not configured; run checks locally before release.
