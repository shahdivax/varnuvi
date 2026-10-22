import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/screens/transcription_settings_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('dev.voiceagent.app/bridge');
  tearDown(() => TestDefaultBinaryMessengerBinding
      .instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, null));

  testWidgets('server starts empty and saves user input before testing',
      (tester) async {
    final calls = <String>[];
    Map? saved;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      calls.add(call.method);
      if (call.method == 'getTranscriptionSettings') {
        return {'endpoint': '', 'hasToken': false};
      }
      if (call.method == 'saveTranscriptionSettings') {
        saved = call.arguments as Map;
      }
      if (call.method == 'testTranscriptionServer') return 'Server connected.';
      return null;
    });
    await tester
        .pumpWidget(const MaterialApp(home: TranscriptionSettingsScreen()));
    await tester.pumpAndSettle();
    expect(find.text('https://x.xx.xxx/api/transcribe'), findsOneWidget);
    await tester.tap(find.text('Save & test connection'));
    await tester.pumpAndSettle();
    expect(find.text('Enter your server URL and its access token.'),
        findsOneWidget);
    expect(saved, isNull);
    await tester.enterText(find.byType(TextField).first,
        'https://speech.example.org/api/transcribe');
    await tester.enterText(find.byType(TextField).last, 'synthetic-' * 5);
    await tester.tap(find.text('Save & test connection'));
    await tester.pumpAndSettle();
    expect(saved?['endpoint'], 'https://speech.example.org/api/transcribe');
    expect(saved?['token'], 'synthetic-' * 5);
    expect(calls.indexOf('saveTranscriptionSettings'),
        lessThan(calls.indexOf('testTranscriptionServer')));
    expect(find.text('Server connected.'), findsOneWidget);
    expect(
        tester.widget<TextField>(find.byType(TextField).last).controller!.text,
        isEmpty);
    await tester.pumpWidget(const SizedBox.shrink());
  });

  testWidgets(
      'changing hosts requires a new token and errors never show success',
      (tester) async {
    var saves = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getTranscriptionSettings') {
        return {
          'endpoint': 'https://speech.example.org/api/transcribe',
          'hasToken': true
        };
      }
      if (call.method == 'saveTranscriptionSettings') saves++;
      if (call.method == 'testTranscriptionServer') {
        throw PlatformException(
            code: 'server_failed', message: 'Could not connect.');
      }
      return null;
    });
    await tester
        .pumpWidget(const MaterialApp(home: TranscriptionSettingsScreen()));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first,
        'https://other.example.org/api/transcribe');
    await tester.tap(find.text('Save & test connection'));
    await tester.pumpAndSettle();
    expect(saves, 0);
    await tester.enterText(find.byType(TextField).last, 'synthetic-' * 5);
    await tester.tap(find.text('Save & test connection'));
    await tester.pumpAndSettle();
    expect(saves, 1);
    expect(find.text('Could not connect.'), findsOneWidget);
    expect(find.textContaining('ready to dictate'), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
