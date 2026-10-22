# Security

Varnuvi is a self-hosted dictation client and a small reference server. It has
not received an independent security audit.

## Boundaries

- No API address, server token, or model-provider key ships in the app.
- Public speech endpoints require HTTPS; redirects are not followed with tokens.
  Plain HTTP is limited to local development addresses.
- Server tokens and optional OpenRouter keys use Android Keystore encryption.
  An unlocked or compromised device remains outside this protection.
- The bubble is an unexported overlay service, with no accessibility service or
  permissions to capture screens or inspect other apps.
- Clipboard output requires an explicit user action.
- The reference server requires a random access token, validates upload size and
  WAV parameters, limits admitted inference work, and cleans temporary files.
  It is designed for personal/small controlled use, not an anonymous public API.
- Keep the service behind HTTPS, restrict network access, rotate tokens, and
  review any reverse-proxy logging and hosting backups.

## Report a vulnerability

Do not post keys, endpoints, recordings, or exploitable details in public issues.
Use GitHub's private vulnerability reporting if enabled for the repository, or
contact the maintainer privately through an established channel. No dedicated
security email or response-time guarantee is provided.

Include the affected version, Android/server environment, expected behavior,
and a synthetic reproduction. Revoke exposed credentials immediately; rewriting
Git history does not revoke a token.
