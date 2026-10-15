# Performance and resource budgets

Varnuvi is intentionally small in dependencies: Flutter is the only runtime Dart
dependency; Android platform APIs handle capture, networking, storage, and the
bubble. There are no embedded speech/LLM weights, analytics, ad SDKs, or continuous
bubble animations. This does not imply zero battery cost or a measured battery
percentage. Audio capture, screen-on time, radio use, and model latency still
cost energy.

## Runtime budget

| Phase | Work and bound |
| --- | --- |
| Screen off | No periodic bubble-recovery callbacks; the microphone is hidden. Accessibility remains bound as Android manages it. |
| No enabled apps | No periodic recovery timer or foreground-window scans from refresh. |
| Enabled apps, bubble dormant | Event-driven updates plus one recovery check every five seconds while the screen is on. |
| Bubble visible | Event-driven updates plus a 500 ms anchor/recovery check. Layout is updated only when coordinates change. |
| Text/content events | Coalesced at 200 ms; a steady event stream cannot indefinitely postpone refresh. Window changes refresh immediately with a short follow-up. |
| Recording | Recovery checks stop. One blocking capture thread; at most two upload workers. The visible-screen contract keeps the display awake while capturing. |
| Recorder display | Real meter samples at about 8 Hz only while recording; textual state updates when the timer second/state changes. Meter loop stops after Submit. |
| Settings status | Two-second poll only while Settings is selected and the app is resumed. |
| Installed apps | Icons are loaded off the UI thread, only when Apps is first opened; list rows are built lazily. |
| Model catalog | Loaded on demand and reused while its settings screen remains open. No periodic model refresh. |
| History | Latest 100 entries. |

PCM conversion reuses a byte buffer. Audio is written incrementally to disk and
streamed into network requests; the client never holds the whole recording in
memory. Each full WAV is 960,044 bytes (about 0.96 MB). At the 30-minute cap,
60 full chunks total about **57.6 MB** before retries. Successful chunks are
deleted promptly. During an outage, up to the session cap can remain as files;
queued tasks are bounded by those 60 chunks, with only two active uploads.
Retrying failed chunks adds network traffic. With Use AI on, model processing sends one assembled transcript after capture,
even with None preferences, so spoken instructions can be followed. With Use AI
off there is no transcript/model request, even with saved preferences. Connection
tests are explicit paid requests independent of the switch. Model processing
does not run on each audio chunk.

## Measured release build

Local build on **15 October 2026**, Flutter 3.47.7 / Dart 3.13.5, JDK 21,
Gradle 8.14.3 / AGP 8.11.1 / Kotlin 2.2.20:

```sh
flutter build apk --release --target-platform android-arm64 --analyze-size
```

| Measurement | Result |
| --- | --- |
| Unsigned ARM64 APK archive | 17,768,634 bytes (17.77 MB / 16.95 MiB) |
| Native libraries | ARM64 `libapp.so` and `libflutter.so` only |
| Flutter assets, uncompressed ZIP entries | 143,063 bytes |
| Material icon font after tree shaking | 5,336 bytes (from 1,645,184) |
| Brand preview images packaged | None |

This is a local size-analysis build, not an installed-size, store-download,
battery, or latency result. Sizes can change with compiler, symbols, signing,
and code. The Flutter runtime/native libraries dominate the archive; removing
small UI details will not remove that baseline. R8 code/resource shrinking is
enabled for release. ABI-specific APKs avoid shipping other CPU architectures.
Flutter's [size guide](https://docs.flutter.dev/perf/app-size) explains why debug
builds and store downloads are different measurements; Android documents
[R8 optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).

## Reproduce and profile

1. Build the same target/version and record exact bytes and the Flutter size
   analysis. Inspect `build/app/outputs/flutter-apk/app-release.apk` with APK
   Analyzer or a ZIP reader. Keep `pubspec.lock` fixed for comparisons.
2. Use a profile build (`flutter run --profile -d <device-id>`) or signed release
   for runtime testing. Debug/JIT overhead is not a production baseline.
3. Record device, Android/OEM version, brightness, network, battery level,
   temperature, enabled apps, build mode, and duration. Compare equal intervals
   in each state: Home/dormant, eligible app/visible, screen off, recording,
   uploads, and optional model work.
4. Inspect process memory with `adb shell dumpsys meminfo dev.voiceagent.app`;
   inspect CPU with Android Studio Profiler/System Trace or `adb shell top`.
   Use Android's battery tools over a meaningful interval. Avoid attributing
   whole-device battery changes solely to Varnuvi.
5. Repeat wake/app-switch/shortcut-movement checks after any timing change.
   Confirm no microphone after leaving enabled apps, no scan timer with screen
   off, and immediate normal event-driven wake-up in eligible apps.

Current evidence includes automated timing/cap tests, a release build/size check,
and Pixel functional checks. A short debug-service smoke sample on Home observed
9 process CPU ticks over 10.12 seconds; this is not a controlled before/after or
battery result. A release-mode ARM64 preview was subsequently installed on the
Pixel using the existing development certificate, preserving the installation.
This preview is not signed for public distribution. A controlled release-mode battery/thermal soak,
low-memory endurance run, and multi-OEM profile remain open. No claim of a
specific battery reduction or universally "production-ready" status is made.
