# Privacy

Applies to the private development preview, 0.1.0. Updated 15 October 2026.
This describes the client implementation; it does not promise how independently
operated transcription or model servers retain data.

## Recording and transcription

Microphone capture starts with an explicit user action and Android permission.
The visible recording screen captures 16 kHz, mono, 16-bit PCM WAV audio. Full
30-second chunks upload while you speak; Submit flushes the final partial chunk.
Sessions stop at 30 minutes. Leaving the screen stops microphone capture.

Audio is sent to the currently configured transcription server. This preview
uses HTTP, so the audio and response are not encrypted in transit. Do not use it
for sensitive recordings. The server returns the displayed `text`; server
logging and retention are outside this client's control. Cancellation disconnects
client requests but cannot recall data already received by a server.

Successfully processed local chunks are deleted. Failed chunks remain available
for retry while the session is open. Normal completion/cancellation deletes the
session directory after capture and uploads stop. Process termination can leave
cache files until Android clears the cache or you clear app storage. Active
sessions are not recovered after a process kill.

## Optional model processing

Only when **Use AI** is switched on, the complete assembled text and relevant
preferences are sent over HTTPS to OpenRouter using your key and selected model. OpenRouter and its routed model provider apply their own data
policies. AI starts off and can be enabled only after a successful connection
test. When off, saved output preferences never trigger a model request. When on,
all None/follow input still invokes AI so it can follow spoken instructions;
without an instruction it is asked to preserve the transcript. Spoken language
is always handled by the transcription server. The switch is captured when a
recording starts, so changing it affects the next recording.

The connection test asks for two bullet points from a fixed synthetic example,
not your dictation, and displays the actual model output. The test itself makes
a model request even with Use AI off. Changing the saved key/model disables AI
and requires testing again before it can be enabled.
Model search retrieves a public catalog. Both require network connectivity;
model tests and processing can consume provider credits.

The OpenRouter key is encrypted with an AES-GCM key in Android Keystore and
stored in app-private preferences. Saved keys are not returned to Flutter when
settings load or forwarded to the transcription server. An unlocked/compromised
device or malicious system software remains outside this protection.

## Accessibility and clipboard

Accessibility is optional for in-app dictation. For the bubble, Android grants
access to window metadata and accessible nodes. The service uses foreground-app
identity, shortcut position, focused-field content, and selection to decide
where the microphone belongs and to insert text safely. Screen content is not
sent to the transcription or model endpoints. Password fields are rejected.

Insertion first tries Android's text action. A clipboard fallback may briefly
put the transcript on the clipboard. The previous item is restored when Android
allows reading it and the clipboard has not since changed. Android and other
clipboard readers can still observe an item while it is present.

## Local data and deletion

The latest 100 saved dictations, enabled-app list, appearance, and writing
preferences stay in app-private device storage. These preferences and history
are not independently encrypted by Varnuvi. Android backup is disabled. There
are no analytics SDKs, advertising, accounts, or cloud history synchronization.

Use History's confirmed clear action to remove saved text, and OpenRouter's
remove-key action to remove its saved credential. Clear Android app storage or
uninstall to remove local app data and caches. Deleting local data does not
delete copies already sent to remote providers.
