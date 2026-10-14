import '../models/transcript_mode.dart';
import 'agent_provider.dart';

/// Returns the text unchanged for every mode. No network, no model.
class MockAgentProvider implements AgentProvider {
  const MockAgentProvider();

  @override
  Future<String> transform(
    String text,
    TranscriptMode mode, {
    String? targetLanguage,
  }) async =>
      text;
}
