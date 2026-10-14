import 'transcript_mode.dart';

/// One past dictation. Mock data only in this phase.
class Transcript {
  const Transcript({
    required this.text,
    required this.mode,
    required this.createdAt,
  });

  final String text;
  final TranscriptMode mode;
  final DateTime createdAt;
}
