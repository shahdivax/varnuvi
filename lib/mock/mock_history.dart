import '../models/transcript.dart';
import '../models/transcript_mode.dart';
import 'mock_data.dart';

/// Hardcoded history entries, newest first. No storage in this phase.
List<Transcript> mockHistory() {
  final now = DateTime.now();
  return [
    Transcript(
      text: kSampleTranscript,
      mode: TranscriptMode.raw,
      createdAt: now.subtract(const Duration(minutes: 5)),
    ),
    Transcript(
      text: 'Hello team, the draft is ready for your review. '
          'I will share the final version by Friday.',
      mode: TranscriptMode.professional,
      createdAt: now.subtract(const Duration(hours: 2)),
    ),
    Transcript(
      text: '• Draft is ready for review\n• Final version by Friday',
      mode: TranscriptMode.bullets,
      createdAt: now.subtract(const Duration(days: 1)),
    ),
    Transcript(
      text: 'Hola equipo, el borrador está listo para revisión '
          'y compartiré la versión final el viernes.',
      mode: TranscriptMode.translate,
      createdAt: now.subtract(const Duration(days: 2)),
    ),
    Transcript(
      text: 'Draft ready; final version due Friday.',
      mode: TranscriptMode.summary,
      createdAt: now.subtract(const Duration(days: 4)),
    ),
  ];
}
