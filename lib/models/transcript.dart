import 'transcript_mode.dart';

/// A locally saved dictation.
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
