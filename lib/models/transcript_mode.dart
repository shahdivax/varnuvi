/// Output modes a transcript can be shaped into.
enum TranscriptMode {
  raw('Raw'),
  professional('Professional'),
  translate('Translate'),
  bullets('Bullets'),
  summary('Summary');

  const TranscriptMode(this.label);

  final String label;
}
