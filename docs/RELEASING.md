# Release checklist

A public build must contain no operator-specific API address or credentials.
The first self-hosted release is v0.1.0, Android base build 7. Keep the base build
number increasing even when resetting human-facing preview version names.

## Build

1. Complete [Development](DEVELOPMENT.md) checks and inspect staged changes.
2. Run `python3 scripts/check_public_source.py`; inspect all assets for private
   data. Scan the history/references that will actually be published as well.
3. Store your upload keystore outside Git. Copy `android/key.properties.example`
   to ignored `android/key.properties`, entering its four properties locally.
4. Build `flutter build apk --release --split-per-abi`.
5. Verify each APK using Android SDK `apksigner verify --verbose --print-certs`.
   Confirm the intended release certificate, not Android Debug.
6. Confirm the packaged manifest has no accessibility service or debug fixture,
   and no endpoint/token is bundled in code, resources, native libraries or assets.
7. Generate SHA-256 checksums and upload the signed APKs plus checksums as release
   assets. Never upload keystores, local config, logs, recordings, or private backups.

Without signing properties, release outputs remain unsigned; debug signing is
never silently substituted. ABI-specific Android version codes incorporate the
base build number. Use ARM64 for most recent phones and verify upgrade behavior
with the real certificate/version code.

A release-signed APK cannot update a debug-signed installation. Do not uninstall
an existing test app or destroy local History/keys without preserving what the
user needs. Signing a build is not proof of a security audit or store approval.

## Validate

- Fresh install with no connection; user-configured HTTPS + authentication;
  localhost USB forwarding; invalid/wrong/unavailable server behavior.
- Actual short speech, chunk boundaries, retry/cancel, and a long-session soak.
- Optional AI off/on using the tester's own settings.
- Bubble lock/unlock, explicit copy/paste and permission revocation.
- Upgrade/persistence and at least one other Android/OEM configuration.
- A real backend WAV smoke test and capacity measurement on the deployment host.

## Publication cleanup

Replacing files does not remove old Git objects, branches, tags, pull-request
references, cached commit views, release assets, forks, or external clones.
Before changing visibility, remove stale published artifacts and obsolete refs,
rotate any exposed credentials, and confirm remaining history is safe. Rewritten
histories require collaborators to re-clone to avoid reintroducing old objects.
GitHub may retain pull-request refs/caches that require its support process; see
[GitHub's sensitive-data removal guidance](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository).
Keep private recovery material outside the published tree and release assets.
