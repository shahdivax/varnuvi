# Host your own Varnuvi speech API

This guide runs the single upload-based transcription API used by the Android
app. Audio is processed with faster-whisper on your machine. The server does
not require OpenRouter, AWS API keys, a streaming service, or a second AI API.

All addresses, domains, and tokens below are examples. `x.xx.xxx` is a masked
placeholder, not a working server. Never put your actual endpoint credentials
in a public fork, screenshot, Git commit, or release asset.

## Choose a setup

| Setup | Network connection |
| --- | --- |
| Personal PC, quickest development test | Bind to loopback and forward USB port 8000 |
| Personal PC for everyday use | Bind to loopback and use a trusted HTTPS tunnel/private network |
| EC2 | Bind to loopback behind an HTTPS reverse proxy |

Use Python 3.11 or 3.12 on a supported machine. The CPU defaults are `small` with
`int8`; no NVIDIA GPU is required. Allow disk space for Python dependencies and
model downloads, and start with several GB of available RAM. Larger models need
more memory. Capacity and latency depend on the machine and language: measure a
30-second recording on your hardware before choosing a model or paying for a
long-running instance. Model downloads require internet access on first use.

## Personal PC

### Linux or macOS

From your checkout:

```sh
cd server
python3 -m venv .venv
. .venv/bin/activate
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
cp .env.example .env
python -c "import secrets; print(secrets.token_hex(32))"
```

Copy the generated token into `.env` as `TRANSCRIPTION_TOKEN`. Keep this file
private. It is ignored by Git. Your token should be a random printable ASCII
string of at least 32 characters, without spaces; the app accepts up to 512.

```dotenv
TRANSCRIPTION_TOKEN=replace-with-your-own-random-token
WHISPER_MODEL=small
WHISPER_DEVICE=cpu
WHISPER_COMPUTE_TYPE=int8
```

The literal `replace-...` token is deliberately rejected by the server. Generate
a real one. No secret is provided by the project.

Start the service from `server/`:

```sh
python -m uvicorn app:app --env-file .env --host 127.0.0.1 --port 8000 --workers 1 --limit-concurrency 8 --no-access-log
```

Keep this terminal open and the PC awake. One worker keeps one model in memory.
The first transcription loads/downloads the model; later requests reuse it.
To pre-download the CPU model before using the phone:

```sh
python -c "from faster_whisper import WhisperModel; WhisperModel('small', device='cpu', compute_type='int8')"
```

Use the model name and settings you selected if they differ from the defaults.
This downloads from the model provider, so its availability and terms apply.

### Windows PowerShell

Install Python 3.11/3.12 and Git, clone the repository, then:

```powershell
cd server
py -3 -m venv .venv
.venv\Scripts\python.exe -m pip install --upgrade pip
.venv\Scripts\python.exe -m pip install -r requirements.txt
Copy-Item .env.example .env
.venv\Scripts\python.exe -c "import secrets; print(secrets.token_hex(32))"
```

Edit `.env` with the generated token, then run:

```powershell
.venv\Scripts\python.exe -m uvicorn app:app --env-file .env --host 127.0.0.1 --port 8000 --workers 1 --limit-concurrency 8 --no-access-log
```

This avoids PowerShell activation-policy changes. Keep the process running.

### Verify the server

In a second Bash terminal (run `bash` first on macOS), use your token. `read -s` keeps it out of
shell history; on Windows use `$env:TRANSCRIPTION_TOKEN` and `curl.exe`.

```sh
read -r -s -p 'Server token: ' TRANSCRIPTION_TOKEN
export TRANSCRIPTION_TOKEN
printf '\n'
curl --fail --show-error -H "Authorization: Bearer $TRANSCRIPTION_TOKEN" http://127.0.0.1:8000/health
```

Expected JSON: `{"status":"ok","model_loaded":false}` before the first inference,
or `true` after it. This verifies authentication and HTTP reachability, not model
accuracy or readiness. Without the header, the result must be HTTP 401.

For a full inference check, supply a real short **mono, 16 kHz, 16-bit PCM WAV**:

```sh
curl --fail --show-error \
  -H "Authorization: Bearer $TRANSCRIPTION_TOKEN" \
  -F 'language=auto' -F 'audio=@sample.wav;type=audio/wav' \
  http://127.0.0.1:8000/api/transcribe
```

Expected shape: `{"text":"Your spoken words","language":"en"}`. A successful
health check alone is not a transcription test. Use non-sensitive test audio.

## Connect over USB

This is a development option, not the everyday connection if you want USB
debugging turned off for other apps.

1. Install Android platform tools on the PC. Enable USB debugging on your phone,
   connect it, and approve the computer on the phone.
2. Confirm the phone with `adb devices`.
3. Run `adb reverse tcp:8000 tcp:8000`.
4. In Varnuvi → **Settings → Speech server**, enter
   `http://localhost:8000/api/transcribe` and the token from `.env`.
5. Choose **Save & test connection**, then dictate a short sentence.

Here, phone `localhost` is forwarded over USB to the PC. Without forwarding,
`localhost` refers to the phone, not your PC. Reconnect/re-run the command if
forwarding is lost. Remove it with `adb reverse --remove tcp:8000`.

Android rejects plain HTTP to arbitrary LAN/public addresses in this build.
Do not replace localhost with a public IP and disable transport security.
The emulator's `http://10.0.2.2:8000/api/transcribe` is also supported for local
development. Public addresses must use HTTPS.

## Connect over HTTPS

For a personal PC without router port forwarding, one option is Tailscale Serve:

1. Install Tailscale on the PC and Android phone and join your private tailnet.
2. Keep the API listening on `127.0.0.1:8000`.
3. On the PC, run `tailscale serve --bg http://127.0.0.1:8000`.
4. Follow its HTTPS-enablement prompt if needed. Use the HTTPS address it prints,
   appending `/api/transcribe`, in Varnuvi. Enter the same API token.
5. Test from the phone while its Tailscale connection is active.

Use **Serve**, which is scoped to your tailnet; publicly exposing the API is not
necessary. Tailnet access rules also apply. Stop sharing with
`tailscale serve reset`. See the provider's [Serve documentation](https://tailscale.com/docs/features/tailscale-serve)
and [CLI reference](https://tailscale.com/docs/reference/tailscale-cli/serve).
You can use another trusted HTTPS reverse proxy instead. Android must trust its
certificate; a self-signed certificate is not automatically accepted.

## EC2

This reference setup uses Ubuntu 24.04 x86_64, CPU inference, a domain you control,
and Caddy for HTTPS. GPU setup is optional and separate below. Nothing in the app
needs AWS credentials. This repository does not provision or bill an instance.

### 1. Instance and networking

Create your own instance with enough RAM/disk for the chosen model, and retain
its SSH key privately. Point a DNS record such as `speech.example.com` to its
public address. Use a stable address/DNS plan if the instance may restart.

Security group inbound rules:

| Port | Source | Purpose |
| --- | --- | --- |
| TCP 22 | Your administrator IP only | SSH |
| TCP 80 | Internet, or your chosen certificate-validation path | HTTP redirect/certificate provisioning |
| TCP 443 | Your phone's allowed network, or internet with token authentication | HTTPS |
| TCP 8000 | **No inbound rule** | API stays bound to loopback |

Keep your account/network policies in mind. Do not open every port. AWS documents
[security-group rules](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/security-group-rules-reference.html).

### 2. Install and configure

SSH into your instance, then:

```sh
sudo apt update
sudo apt install -y git python3-venv python3-pip caddy
sudo useradd --system --create-home --home-dir /opt/varnuvi --shell /usr/sbin/nologin varnuvi
sudo git clone https://github.com/shahdivax/varnuvi.git /opt/varnuvi/repo
sudo chown -R varnuvi:varnuvi /opt/varnuvi
sudo -u varnuvi python3 -m venv /opt/varnuvi/venv
sudo -u varnuvi /opt/varnuvi/venv/bin/python -m pip install -r /opt/varnuvi/repo/server/requirements.txt
sudo cp /opt/varnuvi/repo/server/.env.example /opt/varnuvi/server.env
sudo chmod 600 /opt/varnuvi/server.env
python3 -c "import secrets; print(secrets.token_hex(32))"
sudo nano /opt/varnuvi/server.env
```

Insert that generated token in `server.env`; leave CPU defaults for the first
run. The root-owned environment file is read by systemd, not published by Git.
This clone command assumes the repository has been made public; while private,
use your normal authenticated Git access without embedding credentials in URLs.

### 3. Run as a service

Create `/etc/systemd/system/varnuvi-speech.service` with:

```ini
[Unit]
Description=Varnuvi speech API
After=network-online.target
Wants=network-online.target

[Service]
User=varnuvi
Group=varnuvi
WorkingDirectory=/opt/varnuvi/repo/server
EnvironmentFile=/opt/varnuvi/server.env
Environment=HF_HOME=/opt/varnuvi/.cache/huggingface
ExecStart=/opt/varnuvi/venv/bin/python -m uvicorn app:app --host 127.0.0.1 --port 8000 --workers 1 --limit-concurrency 8 --no-access-log
Restart=on-failure
RestartSec=5
PrivateTmp=true
NoNewPrivileges=true
UMask=0077

[Install]
WantedBy=multi-user.target
```

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now varnuvi-speech
sudo systemctl status varnuvi-speech
sudo journalctl -u varnuvi-speech -n 50 --no-pager
```

Use the authenticated localhost health test above on the instance. If using
`sudo` to inspect environment files, do not copy their contents into logs or issues.
The first model download can take time. To pre-download as the service user:

```sh
sudo -u varnuvi env HF_HOME=/opt/varnuvi/.cache/huggingface /opt/varnuvi/venv/bin/python -c "from faster_whisper import WhisperModel; WhisperModel('small', device='cpu', compute_type='int8')"
```

### 4. Add HTTPS

Replace the Caddyfile site's domain with **your actual domain**. Example
`/etc/caddy/Caddyfile`:

```caddyfile
speech.example.com {
    request_body {
        max_size 2MiB
    }
    reverse_proxy 127.0.0.1:8000
}
```

```sh
sudo caddy validate --config /etc/caddy/Caddyfile
sudo systemctl reload caddy
```

DNS must resolve to your instance and the required ports must be reachable for
certificate issuance. If your packaged Caddy cannot parse the configuration,
install its current stable package using the [official installation guide](https://caddyserver.com/docs/install#debian-ubuntu-raspbian).
Do not bypass a certificate error in the app. Caddy's
[body-size limit](https://caddyserver.com/docs/caddyfile/directives/request_body)
adds a limit before uploads reach the Python server.

From your PC, test `https://speech.example.com/health` with the Authorization
header, then test one WAV upload over HTTPS. In the app use:

```text
API URL: https://speech.example.com/api/transcribe
Access token: the value you generated for TRANSCRIPTION_TOKEN
```

The example domain is not provided by Varnuvi. Do not publish your server token.

### 5. Keep it running

Monitor disk space, memory and model latency. Update dependencies deliberately,
restart the service after configuration changes, and retest a WAV upload.
Rotate the token by changing `server.env`, restarting the service, and updating
the app. Restrict access or stop the instance when it is not needed. Review cloud
resource costs in your account rather than assuming the deployment is free.

## Optional Docker (CPU)

With Docker installed, from the repository root:

```sh
docker build -t varnuvi-speech ./server
# First create server/.env and replace its example token as described above.
docker run -d --name varnuvi-speech --restart unless-stopped \
  --env-file server/.env -p 127.0.0.1:8000:8000 \
  -v varnuvi-models:/home/varnuvi/.cache/huggingface varnuvi-speech
```

The image runs as an unprivileged user. Only the model cache persists in the
named volume; temporary request audio is removed after processing. The container
runs one worker. Put the same HTTPS reverse proxy in front for remote use.
The CPU image does not include NVIDIA runtime libraries or GPU passthrough.

## Optional NVIDIA GPU

The Python server can use a compatible NVIDIA host with these environment values:

```dotenv
WHISPER_MODEL=large-v3-turbo
WHISPER_DEVICE=cuda
WHISPER_COMPUTE_TYPE=float16
```

Install the matching NVIDIA driver, CUDA/cuBLAS and cuDNN libraries before
starting the service. Current faster-whisper/CTranslate2 versions require CUDA
12 and cuDNN 9; see the upstream [GPU requirements](https://github.com/SYSTRAN/faster-whisper#gpu).
A working `nvidia-smi` alone does not establish that all inference libraries are
installed. Keep one worker unless you have measured capacity for multiple model
copies. This repository's deployment examples use the CPU path; GPU deployment
requires validation on your own host.

## API contract

Both routes require `Authorization: Bearer <your-token>`. The token is the one
you generated, not an OpenRouter/AWS credential.

### `POST /api/transcribe`

Send `multipart/form-data` with a valid `Content-Length` and:

| Field | Type | Meaning |
| --- | --- | --- |
| `audio` | File | Mono 16 kHz, 16-bit PCM WAV, at most 31 seconds |
| `language` | Form text | `auto` (default) or a supported Whisper language code, e.g. `en`, `gu`, `hi` |

Total request size is limited to **2 MiB**, including multipart fields. Varnuvi
sends 30-second chunks (~960 KB each), not an entire 30-minute session in one
request. Server inference is serialized; up to two requests are admitted so the
app's two upload workers can coexist. Further requests receive 429 and can be
retried. Keep the external service concurrency limit when deploying.

Successful JSON must contain a string `text`; `language` is also returned.
Silence may return an empty string; the Android app reports that no speech was
returned. Errors use HTTP 401 (auth), 413 (size/missing length), 422 (invalid
WAV/language), 429 (busy), or 503 (model unavailable). Internal exceptions,
recorded audio and transcripts are not included in API error responses.

There is no `polish`, streaming, timestamp, or server-side AI-cleanup API.
Optional AI writing happens separately in the Android client after chunks are
assembled, using the user's own OpenRouter settings.

### `GET /health`

Authenticated JSON: `{"status":"ok","model_loaded":false}`. It does not trigger
a model download or inference. Test an actual WAV to confirm the model works.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Connection refused | Service running, correct port, correct PC/EC2 host, active USB forwarding or HTTPS path |
| 401 | The app and server tokens must match exactly; do not include `Bearer ` in the app's token field |
| Invalid/placeholder URL | Replace `x.xx.xxx`; include `/api/transcribe`; use HTTPS remotely |
| HTTPS/certificate failure | Correct domain/DNS, valid trusted certificate, device clock; do not use a bare HTTP public IP |
| First request takes a long time | Pre-download the model; check server internet access, cache permissions, RAM and model size |
| 503 | Check model/cache permissions and CPU/GPU configuration; perform a local WAV smoke test |
| 422 | Use supported language and mono 16 kHz PCM16 WAV; generic M4A/MP3 uploads are not accepted |
| 429 | Wait for current inference; retry the failed chunk; reduce load or choose faster hardware/model |
| Phone says localhost is unreachable | `adb reverse` must run against that phone; localhost otherwise means the phone itself |

The Kotlin client waits up to 180 seconds for a chunk response. If your host
cannot keep up with 30-second chunks, choose a smaller model or faster hardware
and repeat a real recording test. Do not mistake the model-free automated tests
for evidence of transcription speed or accuracy.
