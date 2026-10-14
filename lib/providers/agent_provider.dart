import '../models/transcript_mode.dart';

/// Future agent backend that reshapes a transcript (paraphrase, translate,
/// bullets, summary). Swappable: bring your own provider and key.
/// Only [MockAgentProvider] exists in this phase.
abstract interface class AgentProvider {
  Future<String> transform(
    String text,
    TranscriptMode mode, {
    String? targetLanguage,
  });
}
