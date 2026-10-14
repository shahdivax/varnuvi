import 'package:flutter/foundation.dart';

import '../mock/mock_languages.dart';
import '../models/transcript_mode.dart';

/// User preferences for dictation. In-memory only for now.
class AppSettings {
  final defaultMode = ValueNotifier<TranscriptMode>(TranscriptMode.raw);
  final translationLanguage = ValueNotifier<String>(kMockLanguages.first);
}

/// Single app-wide instance.
final appSettings = AppSettings();
