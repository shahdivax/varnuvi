import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/widgets/accessibility_setup_card.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('dev.voiceagent.app/bridge');
  late Map<String, dynamic> status;
  var openedSettings = false;
  var settingsBlocked = false;
  setUp(() {
    status = {
      'microphone': false,
      'enabled': false,
      'connected': false,
      'visible': false,
      'appCount': 0
    };
    openedSettings = false;
    settingsBlocked = false;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getBubbleStatus') return status;
      if (call.method == 'requestMicrophone') {
        status['microphone'] = true;
        return true;
      }
      if (call.method == 'openAccessibilitySettings') {
        if (settingsBlocked) {
          throw PlatformException(code: 'settings_blocked');
        }
        openedSettings = true;
      }
      return null;
    });
  });
  tearDown(() => TestDefaultBinaryMessengerBinding
      .instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, null));
  testWidgets('setup advances from microphone to accessibility approval',
      (tester) async {
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AccessibilitySetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Allow microphone'));
    await tester.pumpAndSettle();
    expect(find.text('Enable bubble'), findsOneWidget);
    await tester.tap(find.text('Enable bubble'));
    await tester.pumpAndSettle();
    expect(openedSettings, isTrue);
    await tester.pumpWidget(const SizedBox.shrink());
  });
  testWidgets('permission alone is not reported as an active bubble',
      (tester) async {
    status = {
      'microphone': true,
      'enabled': true,
      'connected': false,
      'visible': false,
      'appCount': 2
    };
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AccessibilitySetupCard())));
    await tester.pumpAndSettle();
    expect(find.text('Service disconnected'), findsOneWidget);
    expect(find.text('Bubble ready'), findsNothing);
    status['connected'] = true;
    await tester.tap(find.byTooltip('Refresh bubble status'));
    await tester.pumpAndSettle();
    expect(find.text('Bubble ready'), findsOneWidget);
    status['visible'] = true;
    await tester.pump(const Duration(seconds: 2));
    await tester.pumpAndSettle();
    expect(find.text('Bubble visible'), findsOneWidget);
    status['dictating'] = true;
    await tester.tap(find.byTooltip('Refresh bubble status'));
    await tester.pumpAndSettle();
    expect(find.text('Dictation in progress'), findsOneWidget);
    await tester.pumpWidget(const SizedBox.shrink());
  });
  testWidgets(
      'settings launch failure does not invalidate a successful status check',
      (tester) async {
    status['microphone'] = true;
    settingsBlocked = true;
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AccessibilitySetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enable bubble'));
    await tester.pumpAndSettle();
    expect(find.text('Status unavailable'), findsNothing);
    expect(find.text('Set up your bubble'), findsOneWidget);
    expect(find.textContaining('Could not open Android settings.'),
        findsOneWidget);
    settingsBlocked = false;
    await tester.tap(find.text('Enable bubble'));
    await tester.pumpAndSettle();
    expect(openedSettings, isTrue);
    expect(
        find.textContaining('Could not open Android settings.'), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
  });
  testWidgets('hidden settings stop status polling and resume when shown',
      (tester) async {
    var calls = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getBubbleStatus') {
        calls++;
        return status;
      }
      return null;
    });
    await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: AccessibilitySetupCard(active: false))));
    await tester.pump(const Duration(seconds: 10));
    expect(calls, 0);
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AccessibilitySetupCard())));
    await tester.pumpAndSettle();
    expect(calls, 1);
    await tester.pump(const Duration(seconds: 2));
    await tester.pumpAndSettle();
    expect(calls, 2);
    await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: AccessibilitySetupCard(active: false))));
    await tester.pump(const Duration(seconds: 10));
    expect(calls, 2);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
