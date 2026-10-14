import '../mock/mock_data.dart';
import 'speech_to_text_provider.dart';

/// Ignores the audio and returns the hardcoded sample sentence.
class MockSpeechToTextProvider implements SpeechToTextProvider {
  const MockSpeechToTextProvider();

  @override
  Future<String> transcribe(List<int> audio, {String? languageCode}) async =>
      kSampleTranscript;
}
