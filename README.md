<p align="center">
  <img src="assets/brand/varnuvi-mark.png" width="80" alt="Varnuvi" />
</p>

# Varnuvi

A small Android dictation app with a floating microphone. Speak, review your
words, copy, and paste wherever you need them.

**Bring your own speech server.** Varnuvi ships with no transcription address,
access token, or hosted service. The included Python server runs faster-whisper
on your own PC or EC2 instance. Optional AI writing uses your own OpenRouter key.

## What it does

- Dictation inside the app or from a draggable floating bubble.
- Continuous recording in 30-second chunks, up to 30 minutes per session.
- Editable results, explicit copy, and device-local history (latest 100 entries).
- Light, dark, and system appearance.
- Optional translation, summaries, tone, and formatting through OpenRouter.

The bubble uses **Display over other apps**, with manual show/hide and a
notification Hide action. There is no accessibility service, screen reading,
app tracking, or automatic insertion into other apps. Paste is always your choice.

## Get started

1. Download the APK for your device from [Releases](https://github.com/shahdivax/varnuvi/releases).
   Most recent Android phones use **arm64-v8a**. Android 13 or newer is required.
2. [Run your speech server](docs/SELF_HOSTING.md) on a personal PC or EC2.
3. Open **Settings → Speech server**. Enter its complete API URL and access
   token, then choose **Save & test connection**.
4. Allow microphone access and start dictation from **Speak**.
5. For the floating shortcut, open **Bubble**, allow **Display over other apps**,
   then tap **Show bubble**. Notifications provide a quick Hide action.
6. Finish recording, review or edit your words, and select **Copy & close**.
   Long-press a text field in your destination app and choose **Paste**.

Example URL (placeholder only): `https://x.xx.xxx/api/transcribe`.
Replace it with your server's real HTTPS address. Nothing is preconfigured.

The bubble stays across apps while enabled. Hide it from the Bubble tab or its
notification. It hides while recording and on the lock screen. Some banking
apps restrict overlays or USB debugging; compatibility varies by device/app.

## Host the API yourself

You need only the included **`POST /api/transcribe`** endpoint. **`GET /health`**
is used to test the connection. No streaming API, timestamps API, OpenRouter key,
AWS credentials, or additional AI service is needed for transcription.

Start with [the self-hosting guide](docs/SELF_HOSTING.md):

| Setup | Guide |
| --- | --- |
| Personal PC, CPU | [Python setup](docs/SELF_HOSTING.md#personal-pc) |
| Phone over USB | [Local development](docs/SELF_HOSTING.md#connect-over-usb) |
| Personal PC over Wi-Fi/internet | [Private HTTPS connection](docs/SELF_HOSTING.md#connect-over-https) |
| EC2 with a domain and HTTPS | [EC2 deployment](docs/SELF_HOSTING.md#ec2) |
| Existing compatible backend | [API contract](docs/SELF_HOSTING.md#api-contract) |

## Optional AI writing

In **Settings → AI**, supply your own OpenRouter API key, choose a model, and
run the connection test. After it succeeds, you can turn **Use AI** on.

AI is off by default. With it off, transcripts never go to OpenRouter. With it
on, the completed transcript and writing preferences go to the selected model.
The connection test uses synthetic text and may consume provider credits.
Changing the key or model requires testing again.

## Privacy

- Audio goes to the speech server you configure, in WAV chunks.
- Public server connections require HTTPS and an access token. HTTP is allowed
  only for localhost/USB forwarding and the Android emulator.
- The included backend requires authentication, validates uploads, and removes
  temporary audio after each request. It does not call a remote AI API.
- Transcripts go to OpenRouter only when you enable AI writing.
- History stays on your device. Tokens use Android Keystore encryption.
- No analytics, ads, account system, cloud history sync, or Android backups.

Your server, reverse proxy, hosting provider, and optional model provider have
separate trust and retention boundaries. See [Privacy](PRIVACY.md) and
[Security](SECURITY.md). This project has not had an independent security audit.

## Build and test

Use Flutter/Dart, JDK 21, and the Android SDK. The app has no third-party runtime
Dart dependencies.

```sh
flutter pub get
flutter analyze
flutter test
flutter build apk --debug
python3 scripts/check_chunking.py
python3 scripts/check_accessibility.py
python3 scripts/check_public_source.py
```

For signed APKs, copy `android/key.properties.example` to the ignored
`android/key.properties` and supply your own keystore. Then build:

```sh
flutter build apk --release --split-per-abi
```

Signing keys, server configuration, tokens, audio, and build outputs must never
be committed. Release builds without signing properties remain unsigned.
See [Development](docs/DEVELOPMENT.md) and [Releasing](docs/RELEASING.md).

## Limits

Varnuvi needs a reachable server for transcription. It does not run a speech
model on the phone. Slow CPUs or a cold model may exceed the request timeout;
failed chunks can be retried while the session remains open. Recording stops
when the recording screen is left. Interrupted sessions are not restored after
Android kills the process. Normal cleanup removes audio, but process crashes
can leave cached files until app storage/cache is cleared.

## Project

[Architecture](docs/ARCHITECTURE.md) · [Self-hosting](docs/SELF_HOSTING.md) ·
[Contributing](CONTRIBUTING.md) · [Changelog](CHANGELOG.md) · [MIT license](LICENSE)
