# Security

Varnuvi is a private development preview, not a security-audited release.
Only the current development version is maintained; no long-term support or
response-time guarantee is offered yet.

## Report privately

Contact the repository owner through the existing private collaboration channel.
If GitHub private vulnerability reporting is enabled on the eventual repository,
use its Security → Report a vulnerability entry. No public reporting address
has been established yet. Do not post exploitable details or secrets in an issue.

Include affected version/commit, Android version, reproduction steps, expected
and actual behavior, and a minimal synthetic example. Do not attach real
recordings, API keys, field text, or unsanitized logs. If a credential leaked,
revoke/rotate it immediately; deleting a commit does not invalidate the key.

## Current boundaries

- The private transcription integration uses HTTP. Audio and responses are not
  protected in transit. HTTPS and external endpoint configuration are required
  before a public release intended for sensitive use.
- The accessibility service is guarded by Android's binding permission. It
  intentionally reads local field state to insert; disabling it removes bubble
  functionality without disabling in-app recording.
- OpenRouter keys use Android Keystore encryption. Device compromise and
  third-party provider retention are outside the client trust boundary.
- Release signing secrets belong outside the repository. Unsigned builds are
  permitted for validation; release builds do not fall back to the debug key.

See [Privacy](PRIVACY.md) for data handling and
[Release preparation](docs/RELEASING.md) for outstanding public-release work.
