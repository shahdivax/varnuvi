/// Future speech-to-text backend. Swappable: bring your own provider and key.
/// Only [MockSpeechToTextProvider] exists in this phase.
abstract interface class SpeechToTextProvider {
  /// Turns one recorded utterance into text.
  Future<String> transcribe(List<int> audio, {String? languageCode});
}
