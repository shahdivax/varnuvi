# Architecture

Varnuvi uses Flutter for its four-tab interface and Kotlin for Android audio,
networking, app discovery, accessibility, storage, and key handling. Runtime Dart
dependencies consist only of the Flutter SDK. There is no embedded speech model,
analytics SDK, state-management framework, or network client package.

## Data path

```text
User starts recording
  → SpeechActivity → ContinuousRecorder (AudioRecord)
  → PcmChunks (16 kHz mono WAV, exact 30-second sample boundaries)
  → TranscriptionApi (two upload workers)
  → ChunkTranscript (ordered results; failed-only retry)
  → optional OpenRouter transform (one assembled transcript)
  → editable in-app output OR original-field insertion
  → local history when requested / automatically for bubble dictation
```

`SpeechActivity` owns the session and its cancellation. Submit is debounced;
recording drains its final partial chunk before completion. Successful chunks
are deleted and never reuploaded on retry. Recording is capped at 28,800,000
samples (30 minutes). Partial output and bypassing failed writing processing
require explicit user choice. The client accepts only the transcription JSON
`text`, never substitutes `raw_text`.

The Use AI switch, output preferences, key, and model are snapshotted at session
start. Only a successfully tested connection with Use AI on runs the model.
None preferences still allow explicit spoken instructions. With the switch off,
no model request or key decryption occurs, regardless of saved defaults.
Connection changes disable AI and invalidate its test. A revision check prevents
a stale test from authorizing a different connection. Changing
settings affects the next recording. If rewriting fails, original text stays
available while that session remains open. Requests have timeouts and are
disconnected on cancellation; at most two transcription workers run per session.

## Bubble lifecycle and insertion

`VoiceAccessibilityService` observes window, content, focus, and selection events.
The allowlist starts empty. Window changes refresh immediately and schedule a
short follow-up; content events coalesce rather than continually postpone work.
Recovery/position checks run every 500 ms while visible, every five seconds
while dormant, and stop when the screen is off, no apps are enabled, or a
recording owns the target. These are Handler callbacks, not wake-up alarms.

The microphone shows in a foreground enabled app. Android's keyboard and the
app's overlay cannot replace that app. A small system-shortcut window can defer
to the focused app; full system screens hide the microphone. When available,
the small SystemUI window bounds anchor the microphone with a gap. OEMs can
expose different window trees; the saved position is the fallback. The system
accessibility shortcut opens the app directly.

At tap time, a visible, editable, enabled, non-password focused node is required.
The target captures a UUID, package, window, node, original text, and selection.
Delivery is accepted once, for that same session and unchanged field. Selection
is restored only when safe. `TextInserter` merges at the cursor/replaces the
selection, tries `ACTION_SET_TEXT`, then clipboard paste only if needed. An
acknowledged insertion is verified once and never blindly repeated. Failures
recover via History when saving succeeds.

## Persistence and UI bridge

`NativeBridge` exposes the `dev.voiceagent.app/bridge` MethodChannel. Installed
app/icon discovery and network work stay off the main thread. Flutter uses native
SharedPreferences-backed settings/history; history is bounded to 100 entries.
The OpenRouter key is encrypted with Android Keystore and never returned through
the channel on settings load.

`IndexedStack` retains tab state. Apps refreshes its enabled list when revisited.
The Settings status card polls only when its tab is selected and the Flutter
app is resumed. Recorder meter samples are visualized at roughly 8 Hz; textual
state updates at timer-second changes or actual session state changes.

## Source map

| Location | Responsibility |
| --- | --- |
| `lib/screens`, `lib/widgets`, `lib/theme` | Flutter interface and visual identity |
| `lib/bridge`, `lib/store` | Native calls and local transcript view state |
| `android/app/src/main/kotlin/dev/voiceagent/app/speech` | Capture, chunks, uploads, session UI, history |
| `…/accessibility`, `…/overlay`, `…/insert` | Foreground detection, bubble, safe insertion |
| `…/writing` | Validated preferences, encrypted credential, OpenRouter |
| `test`, `test/native` | Widget/unit tests, goldens, pure Kotlin regression checks |
| `assets/brand` | Brand source/export assets, not bundled wholesale into the app |

Internal `veya` storage/cache names and `dev.voiceagent.app` IDs are retained for
compatibility. They are implementation details, not the displayed brand.
