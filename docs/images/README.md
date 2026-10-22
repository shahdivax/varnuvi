# README images

These are captures of the current Android app, with synthetic example text.
They contain no real conversations, server addresses, credentials, or recordings.

- `speak-light.png`: an editable sample draft typed into the real Speak screen.
- `bubble-dark.png`: the actual overlay service running on the Bubble screen.
- `history-light.png`, `history-dark.png`: three synthetic notes in local History.
- `server-dark.png`: the unconfigured speech-server form, with no saved token.
- `hero.png`: a designed presentation of the unmodified Speak and History captures,
  framed and rendered from HTML with the existing Varnuvi mark.

The clock and status icons use Android's standard screenshot demo mode. No
microphone, transcription server, or AI provider is used to create these images.
The presentation frames are decorative, not a claim about a particular device.

## Reproduce

Use an isolated emulator with a 1080 × 2400 display and the debug app installed:

```sh
python3 scripts/capture_readme.py --serial emulator-5556
python3 scripts/render_readme.py
```

The capture script **clears the selected emulator's Varnuvi app data** and seeds
synthetic fixtures. It refuses physical-device serials. It requires Android
platform tools. The hero renderer requires Google Chrome; all inputs are local.
App source and behavior are not modified by these scripts.
