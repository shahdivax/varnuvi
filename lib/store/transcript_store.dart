import 'dart:convert';
import 'package:flutter/foundation.dart';
import '../bridge/native_bridge.dart';
import '../models/transcript.dart';
import '../models/transcript_mode.dart';

class TranscriptStore extends ValueNotifier<List<Transcript>> {
  TranscriptStore() : super(const []);
  Future<void> reload() async {
    final items = jsonDecode(await NativeBridge.getHistory()) as List<dynamic>;
    value = [
      for (final item in items)
        Transcript(
            text: item['text'] as String,
            mode: TranscriptMode.raw,
            createdAt:
                DateTime.fromMillisecondsSinceEpoch(item['createdAt'] as int))
    ];
  }

  Future<void> save(String text) async {
    await NativeBridge.saveTranscript(text);
    await reload();
  }

  Future<void> clear() async {
    await NativeBridge.clearHistory();
    value = [];
  }
}

final transcripts = TranscriptStore();
