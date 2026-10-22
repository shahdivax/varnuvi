import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/main.dart';
import 'package:voice_agent/theme/theme_controller.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
            const MethodChannel('dev.voiceagent.app/bridge'), (call) async {
      if (call.method == 'getBubbleStatus') {
        return {
          'microphone': false,
          'enabled': false,
          'connected': false,
          'visible': false,
          'appCount': 0
        };
      }
      if (call.method == 'getHistory') return '[]';
      return null;
    });
  });
  for (final mode in [ThemeMode.light, ThemeMode.dark]) {
    testWidgets('Varnuvi ${mode.name} at phone width', (tester) async {
      tester.view.physicalSize = const Size(412, 915);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      themeController.value = mode;
      await tester.pumpWidget(const VarnuviApp());
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(find.byType(MaterialApp),
          matchesGoldenFile('goldens/veya-${mode.name}.png'));
      await tester.tap(find.text('Bubble'));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      await expectLater(find.byType(MaterialApp),
          matchesGoldenFile('goldens/bubble-${mode.name}.png'));
      await tester.tap(find.text('History'));
      await tester.pumpAndSettle();
      expect(find.text('A fresh start'), findsOneWidget);
      expect(tester.takeException(), isNull);
      await tester.pumpWidget(const SizedBox.shrink());
    });
  }
}
