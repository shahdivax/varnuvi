# Architecture

Flutter owns the four-tab interface. Kotlin uses Android APIs for recording,
HTTPS, overlay windows, Keystore credentials, and private local storage. There
are no third-party runtime Dart dependencies.

## Recording path

```text
User starts recording
→ SpeechActivity → AudioRecord → mono 16 kHz PCM16 WAV chunks
→ two upload workers → configured POST /api/transcribe
→ ordered transcript → optional OpenRouter writing
→ editable result → explicit copy → manual paste
```

`SpeechActivity` snapshots the server connection and writing settings before
recording. An unset server stops setup before microphone capture. WAV chunks
have exact 30-second sample boundaries; recording ends at 30 minutes. Failed-only
retry preserves order without resending successful chunks. Cancellation stops
capture, disconnects requests, and removes session files after workers finish.

In-app results return to Flutter for explicit editing/save/copy. Bubble results
save to History and open a compact editable result panel. Copy & close changes
the clipboard only after a tap. Original bubble results and copied edits save;
closing without copying does not save edits. History is bounded to 100 entries.

## Server configuration

There is no default endpoint. `TranscriptionSettings` stores a validated URL and
Keystore-encrypted token. HTTPS is mandatory for remote hosts; explicit local
development addresses are the only HTTP exceptions. URLs cannot contain embedded
credentials, query strings, or fragments. Switching the host requires entering
that host's token. Token values are never returned to Flutter on load.

`TranscriptionApi` sends fixed-length multipart WAV and language fields, includes
Bearer authentication, does not follow redirects, and accepts a non-empty JSON
`text`. Each request has bounded connection/read timeouts. `GET /health` tests a
saved connection without recording or running inference.

## Overlay

`BubbleService` is unexported and explicitly user-started, with a foreground
notification and Hide action. It uses `TYPE_APPLICATION_OVERLAY`, hides while
recording and on lock, and is not restarted automatically after process death.
There is no accessibility service, app discovery, screen reading, or foreground
app tracking. The user manually shows/hides the bubble.

## Python server

`server/app.py` has one upload endpoint and an authenticated health endpoint.
It lazily loads faster-whisper, serializes inference, admits at most two requests,
checks Content-Length and WAV parameters, and cleans per-request temporary audio.
Run one worker with the documented HTTP concurrency limit. Model download and
inference stay separate from the Android client; no remote cleanup API is used.
The reference deployment is for personal/controlled use, with HTTPS at a proxy.

## Optional writing

AI starts off and requires a tested OpenRouter connection before enabling. Key
or model changes invalidate the test. Only enabled sessions send assembled text
to OpenRouter; no audio is sent there. A revision check prevents a stale test
from authorizing changed credentials. Keystore storage is shared through the
small `EncryptedSecret` utility while keeping separate aliases and preferences.

## Layout

| Location | Purpose |
| --- | --- |
| `lib/screens`, `lib/widgets`, `lib/theme` | Flutter UI |
| `lib/bridge`, `lib/store` | Native calls and transcript state |
| Android `speech/` | Capture, chunks, API connection, session/result UI, History |
| Android `overlay/` | Bubble windows, position, foreground service |
| Android `security/`, `writing/` | Keystore secrets and optional OpenRouter |
| `server/` | Standalone self-hosted transcription backend |
| `test/`, `scripts/` | UI, native, server, permission and publication checks |

Internal storage names and `dev.voiceagent.app` remain stable for installed-app
compatibility. They are not the user-facing brand.
