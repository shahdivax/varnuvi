# Release preparation

The repository stays private for now. The first signed private preview is v0.1.0 (Android build 2), distributed through
the private GitHub repository's release assets. This guide does not authorize
changing repository visibility or uploading to a store.

## Build and sign

1. Run the checks in [Development](DEVELOPMENT.md), review the changelog, and bump
   `version` in `pubspec.yaml`. Keep increasing the Android build number.
2. For distribution, create/secure an upload keystore outside the repository.
   Copy `android/key.properties.example` to ignored `android/key.properties` and
   fill its four properties locally. Keep passwords and backup keys private.
3. Build `flutter build apk --release --split-per-abi` for direct delivery, or
   `flutter build appbundle --release` for an eventual store upload.
4. Verify an APK with your SDK's `apksigner verify --verbose <apk>`. Confirm the
   intended certificate rather than the development/debug certificate. Retain
   signing credentials and any mapping/symbol files privately with the release.

Without signing properties, release builds are unsigned for build/size validation
and cannot be installed as distribution APKs. Debug builds remain debug-signed
for local testing. A production-signed app cannot update a debug-signed install;
plan the migration without silently deleting a user's local history/key.

Prefer ABI-specific APKs to a universal APK. R8 code/resource shrinking and
Flutter icon tree shaking are enabled. Size measurement:

```sh
flutter build apk --release --target-platform android-arm64 --analyze-size
```

Inspect outputs in `build/app/outputs/flutter-apk/` and the Flutter size report.
Archive size, installed size, and store download size are different measurements.

## Before private distribution

- Complete the device regression pass, including an actual 30-minute session,
  recovery during poor connectivity, release-mode insertion, and at least one
  additional Android/OEM configuration.
- Measure CPU, memory, battery, and thermal behavior during idle, dormant,
  listening, upload, and writing phases. Record build/device/time interval.
- Confirm real OpenRouter requests using a user-provided key and each supported
  writing path. Mock UI tests do not establish provider reliability. The current preview has
  passed one live two-bullet summary test with the saved model; other writing
  paths and recording-to-model flows still need their device regression pass.
- Review crash/process-kill recovery limitations and HTTP exposure with testers.
- Keep raw recordings, personal screenshots, credentials, and diagnostic dumps
  out of commits and packaged artifacts.

## Before making the repository public — deferred

- Replace the fixed transcription endpoint with documented user configuration;
  remove private infrastructure references from source, examples, docs, assets,
  artifacts, and **Git history**. Review/rotate any exposed credentials.
- Require HTTPS for public use and review server authentication, retention,
  consent, and operating costs. Document the backend contract using neutral data.
- Configure private vulnerability reporting and maintainer contact details.
- Review accessibility disclosure/consent and applicable distribution policies;
  make sure the privacy document matches the chosen providers and real behavior.
- Update the README status, supported versions, screenshots with synthetic data,
  release notes, license/asset provenance, and signing/distribution instructions.
- Decide whether to enable CI; if enabled, review action pins/permissions and
  validate it on the repository. Scan the entire history with a full secret
  scanner and verify the archive from a clean clone.

These items remain open. A successful release build is not evidence that the
app has completed production validation.

## Maintainer signing backup

The first release uses alias `varnuvi-upload`, RSA 4096, with owner-only files
outside the Git checkout at `/home/pc/.local/share/varnuvi-signing/`. That directory
contains the release keystore and its private signing properties. The ignored
`android/key.properties` points to it. Back up both securely outside the machine;
losing the signing key can prevent future updates to directly installed APKs.
Never commit or upload that directory. Public certificate fingerprints are
included in the release; private key/password material is not.

The current development phone uses the older development certificate. Creating
or downloading this signed release does not migrate that installed app's data.
