# Privacy

Updated 22 October 2026. This describes the app and included server; independently
operated hosts, proxies, and model providers have their own data policies.

## Audio

Recording starts only through an explicit action with microphone permission.
Audio is captured as mono 16 kHz PCM16 WAV and sent in 30-second chunks to the
speech server you configure. No server is bundled or selected for you. Recording
stops when its visible screen is left and is capped at 30 minutes per session.

Remote connections use HTTPS and your access token. HTTP is allowed only for
localhost/USB forwarding and the Android emulator. The included server processes
audio locally with faster-whisper, keeps one reusable model, and removes each
request's temporary audio after processing. It makes no remote AI API call;
model files are downloaded on first use. Its sample deployment disables HTTP
access logs. Hosting providers, reverse proxies, custom modifications, filesystem
snapshots, and process crashes can introduce separate retention.

The Android client deletes successfully processed chunks and normal session
files. Failed chunks remain for retry while the session is open. A process kill
can leave cached audio until Android clears it or app storage/cache is cleared.
Active sessions are not restored after process termination.

## Optional AI writing

Use AI starts off. Only when enabled, the assembled transcript and selected
preferences are sent over HTTPS to OpenRouter using your own key/model. Its
provider policies apply. No audio is sent to OpenRouter. Connection testing sends
synthetic text and can incur provider charges even while Use AI is off. Changing
the key/model invalidates the test and disables AI until tested again.

## Local storage and clipboard

The latest 100 saved transcripts, appearance, bubble position, and preferences
stay in app-private device storage. History is not separately encrypted by the
app. Android backup is disabled. Speech-server and OpenRouter tokens are
Keystore-encrypted and are not returned to Flutter when settings load.

Original bubble transcripts save to History when processing completes. Copied
edits also save; closing without copying does not save edits. A transcript goes
to the clipboard only when you choose Copy or Copy & close, replacing its prior
item. Android and eligible clipboard readers may observe it; normal Android
clipboard expiry applies.

## Overlay

The bubble uses Display over other apps and a user-started foreground service.
The service never records audio itself. It hides the bubble on the lock screen.
No accessibility service, screen capture, field reading, installed-app discovery,
or foreground-app monitoring is used.

## Removal

Clear saved text in History. Remove server credentials in Settings → Speech
server, and remove the AI key in Settings → AI. Clearing Android app storage or
uninstalling removes local app data and caches. Server operators must manage
server data, caches, backups and tokens independently. Local deletion cannot
recall data already sent to another system.

There are no analytics SDKs, ads, accounts, or cloud history synchronization.
