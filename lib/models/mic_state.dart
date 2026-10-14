/// Visual states of the mic. UI only; driven by a fake timer for now.
enum MicState {
  idle('Tap to dictate', 'Start dictation'),
  listening('Listening…', 'Stop dictation'),
  processing('Processing…', 'Processing dictation');

  const MicState(this.status, this.semanticLabel);

  /// Short status line shown under the mic.
  final String status;

  /// Screen reader label for the mic button.
  final String semanticLabel;
}
