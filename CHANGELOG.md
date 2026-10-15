# Changelog

Notable user-facing changes. Unreleased work is not a published release.

## 0.1.0 — 15 October 2026

First signed private Android preview, build 2. APKs and App Bundle are distributed
as private GitHub release assets with SHA-256 checksums.

### Added

- Varnuvi identity, adaptive Android icons, and transparent/square/circle assets.
- Continuous 30-second chunk transcription with a 30-minute session cap,
  two upload workers, ordered assembly, failed-only retry, and partial recovery.
- Measured microphone levels, visible recording controls, editable results,
  copy, and local history capped at 100 entries.
- Per-app microphone bubble, enabled-app grouping, original-field insertion,
  selection preservation, duplicate prevention, and History recovery.
- Saved writing preferences; optional OpenRouter key, model search, and test.
- Persistent appearance, live permission/service status, and direct app shortcut.
- Automated UI/native checks and developer, privacy, security, and release docs.

### Changed

- CI is deferred; its configuration is retained as an inactive documentation
  example rather than an active GitHub workflow.

- Connected AI settings show Retest connection, hide redundant save controls,
  and group key removal under Connection options. Editing key/model restores setup.
- README opens with AI-created product artwork; unmodified device screenshots
  remain available in a collapsed section.

- AI settings now expose an opt-in Use AI switch after a successful model test.
  On follows spoken instructions even with None defaults; off bypasses model
  requests regardless of saved preferences. New keys/models require retesting.
- Connection tests show a real two-bullet summary from synthetic text.
- Apps displays friendly names without package IDs.

- Removed prototype transcripts, simulated recording, and nonfunctional controls.
- Microphone follows the system shortcut when available and recovers after
  foreground/wake changes. The shortcut opens Varnuvi without an intermediate menu.
- Dormant reconciliation backs off; screen-off, recording, and disabled-app states
  stop recovery timers. Hidden Settings tabs stop status polling.
- Reused PCM conversion storage, reduced recorder text updates, and removed the
  unused animated bubble-state implementation.
- Optimized release builds shrink code/resources and require explicit signing
  configuration instead of silently using the debug signing key.

### Known limitations

- Private, fixed HTTP transcription integration; not suitable for sensitive audio.
- Visible-screen recording only; no active-session recovery after process death.
- OEM/custom field insertion and release endurance still require broader testing.
