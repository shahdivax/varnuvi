import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/screens/home_screen.dart';
import 'package:voice_agent/store/transcript_store.dart';
import 'package:voice_agent/theme/app_theme.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('dev.voiceagent.app/bridge');
  var saved = <Map<String, Object>>[];
  setUp(() {
    saved = [];
    transcripts.value = [];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      switch (call.method) {
        case 'recognizeSpeech':
          return 'These are my real words.';
        case 'getHistory':
          return jsonEncode(saved);
        case 'saveTranscript':
          saved.insert(0, {
            'text': (call.arguments as Map)['text'] as String,
            'createdAt': 1000
          });
          return null;
        case 'clearHistory':
          saved.clear();
          return null;
      }
      return null;
    });
  });
  tearDown(() => TestDefaultBinaryMessengerBinding
      .instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, null));
  test('saved dictations reload and clear through native storage', () async {
    final store = TranscriptStore();
    await store.save('Only what I actually said');
    await store.reload();
    expect(store.value.single.text, 'Only what I actually said');
    await store.clear();
    expect(store.value, isEmpty);
    expect(saved, isEmpty);
    store.dispose();
  });
  testWidgets('dictation stays editable and only saves when requested',
      (tester) async {
    await tester.pumpWidget(MaterialApp(
        theme: AppTheme.light, home: const Scaffold(body: HomeScreen())));
    expect(find.text('Your words appear here…'), findsOneWidget);
    await tester.ensureVisible(find.text('Start dictation'));
    await tester.tap(find.text('Start dictation'));
    await tester.pumpAndSettle();
    expect(find.text('These are my real words.'), findsOneWidget);
    expect(saved, isEmpty);
    await tester.enterText(find.byType(TextField), 'An edited thought.');
    await tester.ensureVisible(find.text('Save'));
    await tester.tap(find.text('Save'));
    await tester.pumpAndSettle();
    expect(saved.single['text'], 'An edited thought.');
    expect(find.text('Saved'), findsOneWidget);
  });
  testWidgets('cancelled recognition does not invent a transcript',
      (tester) async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (_) async => null);
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: HomeScreen())));
    await tester.ensureVisible(find.text('Start dictation'));
    await tester.tap(find.text('Start dictation'));
    await tester.pumpAndSettle();
    expect(tester.widget<TextField>(find.byType(TextField)).controller!.text,
        isEmpty);
    expect(saved, isEmpty);
  });
}
