import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/screens/writing_defaults_screen.dart';
import 'package:voice_agent/screens/openrouter_screen.dart';
import 'package:voice_agent/main.dart';
import 'package:voice_agent/bridge/native_bridge.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('dev.voiceagent.app/bridge');
  late Map<String, String> defaults;
  var model = 'provider/first';
  var tested = 0;
  var verified = false;
  var aiEnabled = false;
  setUp(() {
    defaults = emptyWritingDefaults();
    tested = 0;
    verified = false;
    aiEnabled = false;
    model = 'provider/first';
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      switch (call.method) {
        case 'getWritingSettings':
          return {
            'hasKey': true,
            'model': model,
            'defaults': defaults,
            'tested': verified,
            'aiEnabled': aiEnabled
          };
        case 'saveWritingDefaults':
          defaults = Map<String, String>.from(
              (call.arguments as Map)['defaults'] as Map);
          return null;
        case 'saveOpenRouter':
          final next = (call.arguments as Map)['model'] as String;
          if (next != model) {
            verified = false;
            aiEnabled = false;
          }
          model = next;
          return null;
        case 'testOpenRouter':
          tested++;
          verified = true;
          return 'Connected. The selected model returned text.';
        case 'setAiEnabled':
          aiEnabled = (call.arguments as Map)['enabled'] as bool;
          return null;
        case 'getOpenRouterModels':
          return [
            {'id': 'provider/first', 'name': 'First model', 'context': 16000},
            {'id': 'provider/second', 'name': 'Second model', 'context': 32000}
          ];
        case 'consumeDestination':
          return 'settings';
        case 'getHistory':
          return '[]';
        case 'getInstalledApps':
        case 'getEnabledPackages':
          return [];
        case 'getBubbleStatus':
          return {
            'microphone': false,
            'enabled': false,
            'connected': false,
            'visible': false,
            'appCount': 0
          };
      }
      return null;
    });
  });
  tearDown(() => TestDefaultBinaryMessengerBinding
      .instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, null));
  test('all initial choices follow input', () {
    expect(defaults, {
      'action': 'none',
      'inputLanguage': 'auto',
      'outputLanguage': 'auto',
      'tone': 'none',
      'style': 'none'
    });
  });
  testWidgets('selected defaults save and reload from native storage',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: WritingDefaultsScreen()));
    await tester.pumpAndSettle();
    await tester
        .ensureVisible(find.byKey(const ValueKey('inputLanguage-auto')));
    await tester.tap(find.byKey(const ValueKey('inputLanguage-auto')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gujarati').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const ValueKey('tone-none')));
    await tester.tap(find.byKey(const ValueKey('tone-none')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Professional').last);
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Save defaults'));
    await tester.tap(find.text('Save defaults'));
    await tester.pumpAndSettle();
    expect(defaults['inputLanguage'], 'gu');
    expect(defaults['tone'], 'professional');
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpWidget(const MaterialApp(home: WritingDefaultsScreen()));
    await tester.pumpAndSettle();
    expect(find.byKey(const ValueKey('inputLanguage-gu')), findsOneWidget);
    expect(find.byKey(const ValueKey('tone-professional')), findsOneWidget);
  });
  testWidgets(
      'model picker searches and connection test uses the chosen saved model',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: OpenRouterScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Key saved · leave blank to keep it'), findsOneWidget);
    expect(find.text('Use AI'), findsNothing);
    expect(find.text('AI'), findsOneWidget);
    expect(
        tester.widget<TextField>(find.byType(TextField).first).controller!.text,
        isEmpty);
    await tester.ensureVisible(find.text('Search models'));
    await tester.tap(find.text('Search models'));
    await tester.pumpAndSettle();
    await tester.enterText(
        find.widgetWithText(TextField, 'Search models'), 'second');
    await tester.pumpAndSettle();
    expect(find.text('First model'), findsNothing);
    await tester.tap(find.text('Second model'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Save & test connection'));
    await tester.tap(find.text('Save & test connection'));
    await tester.pumpAndSettle();
    expect(model, 'provider/second');
    expect(tested, 1);
    expect(find.text('Connected. The selected model returned text.'),
        findsOneWidget);
    expect(verified, isTrue);
    expect(aiEnabled, isFalse);
    expect(find.text('Retest connection'), findsOneWidget);
    expect(find.text('Save without testing'), findsNothing);
    expect(find.text('Save & test connection'), findsNothing);
    await tester.ensureVisible(find.text('Use AI'));
    await tester.tap(find.text('Use AI'));
    await tester.pumpAndSettle();
    expect(aiEnabled, isTrue);
    expect(tested, 1); // Switching on does not send a test/model request.
    await tester.tap(find.text('Use AI'));
    await tester.pumpAndSettle();
    expect(aiEnabled, isFalse);
    expect(tested, 1);
  });
  testWidgets('connected settings retest without changing AI preference',
      (tester) async {
    verified = true;
    aiEnabled = true;
    await tester.pumpWidget(const MaterialApp(home: OpenRouterScreen()));
    await tester.pumpAndSettle();
    expect(find.text('Save without testing'), findsNothing);
    expect(find.text('Remove saved key'), findsNothing);
    await tester.ensureVisible(find.text('Retest connection'));
    await tester.tap(find.text('Retest connection'));
    await tester.pumpAndSettle();
    expect(tested, 1);
    expect(aiEnabled, isTrue);
    expect(find.text('Retest connection'), findsOneWidget);
    await tester.ensureVisible(find.text('Connection options'));
    await tester.tap(find.text('Connection options'));
    await tester.pumpAndSettle();
    expect(find.text('Remove saved key'), findsOneWidget);
  });
  testWidgets('AI choice persists and changing the model requires another test',
      (tester) async {
    verified = true;
    aiEnabled = true;
    await tester.pumpWidget(const MaterialApp(home: OpenRouterScreen()));
    await tester.pumpAndSettle();
    expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value,
        isTrue);
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpWidget(const MaterialApp(home: OpenRouterScreen()));
    await tester.pumpAndSettle();
    expect(tester.widget<SwitchListTile>(find.byType(SwitchListTile)).value,
        isTrue);
    await tester.ensureVisible(find.widgetWithText(TextField, 'Model ID'));
    await tester.enterText(
        find.widgetWithText(TextField, 'Model ID'), 'provider/changed');
    await tester.pumpAndSettle();
    expect(find.text('Use AI'), findsNothing);
    expect(find.text('Retest connection'), findsNothing);
    expect(find.text('Save & test connection'), findsOneWidget);
    await tester.ensureVisible(find.text('Save without testing'));
    await tester.tap(find.text('Save without testing'));
    await tester.pumpAndSettle();
    expect(verified, isFalse);
    expect(aiEnabled, isFalse);
    expect(tested, 0);
    expect(find.text('Use AI'), findsNothing);
  });
  testWidgets(
      'shortcut destination opens settings rather than silently doing nothing',
      (tester) async {
    await tester.pumpWidget(const MaterialApp(home: VarnuviApp()));
    await tester.pumpAndSettle();
    expect(
        tester.widget<NavigationBar>(find.byType(NavigationBar)).selectedIndex,
        3);
    NativeBridge.destination.value = 'home';
    await tester.pumpAndSettle();
    expect(
        tester.widget<NavigationBar>(find.byType(NavigationBar)).selectedIndex,
        0);
    NativeBridge.destination.value = 'settings';
    await tester.pumpAndSettle();
    expect(
        tester.widget<NavigationBar>(find.byType(NavigationBar)).selectedIndex,
        3);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
