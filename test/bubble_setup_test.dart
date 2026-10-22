import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/widgets/bubble_setup_card.dart';

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
      if (call.method == 'openOverlaySettings') {
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
  testWidgets('setup advances from microphone to overlay approval',
      (tester) async {
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Allow microphone'));
    await tester.pumpAndSettle();
    expect(find.text('Allow floating bubble'), findsOneWidget);
    await tester.tap(find.text('Allow floating bubble'));
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
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    expect(find.text('Bubble hidden'), findsOneWidget);
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
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Allow floating bubble'));
    await tester.pumpAndSettle();
    expect(find.text('Status unavailable'), findsNothing);
    expect(find.text('Set up your bubble'), findsOneWidget);
    expect(find.textContaining('Could not update the bubble.'), findsOneWidget);
    settingsBlocked = false;
    await tester.tap(find.text('Allow floating bubble'));
    await tester.pumpAndSettle();
    expect(openedSettings, isTrue);
    expect(find.textContaining('Could not update the bubble.'), findsNothing);
    await tester.pumpWidget(const SizedBox.shrink());
  });
  testWidgets('show and hide use the overlay service without accessibility',
      (tester) async {
    status['microphone'] = true;
    status['enabled'] = true;
    final actions = <bool>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getBubbleStatus') return status;
      if (call.method == 'setBubbleEnabled') {
        final enabled = (call.arguments as Map)['enabled'] as bool;
        actions.add(enabled);
        status['connected'] = enabled;
      } else {
        fail('Unexpected permission or accessibility call: ${call.method}');
      }
      return null;
    });
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Show bubble'));
    await tester.pumpAndSettle();
    expect(find.text('Hide bubble'), findsOneWidget);
    await tester.tap(find.text('Hide bubble'));
    await tester.pumpAndSettle();
    expect(actions, [true, false]);
    expect(find.text('Show bubble'), findsOneWidget);
    await tester.pumpWidget(const SizedBox.shrink());
  });
  testWidgets('a running bubble can be hidden after microphone is revoked',
      (tester) async {
    status['microphone'] = false;
    status['enabled'] = true;
    status['connected'] = true;
    var hidden = false;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getBubbleStatus') return status;
      if (call.method == 'setBubbleEnabled') {
        expect((call.arguments as Map)['enabled'], false);
        hidden = true;
        status['connected'] = false;
      } else {
        fail('Hide must not require another permission: ${call.method}');
      }
      return null;
    });
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Hide bubble'));
    await tester.pumpAndSettle();
    expect(hidden, isTrue);
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
        home: Scaffold(body: BubbleSetupCard(active: false))));
    await tester.pump(const Duration(seconds: 10));
    expect(calls, 0);
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: BubbleSetupCard())));
    await tester.pumpAndSettle();
    expect(calls, 1);
    await tester.pump(const Duration(seconds: 2));
    await tester.pumpAndSettle();
    expect(calls, 2);
    await tester.pumpWidget(const MaterialApp(
        home: Scaffold(body: BubbleSetupCard(active: false))));
    await tester.pump(const Duration(seconds: 10));
    expect(calls, 2);
    await tester.pumpWidget(const SizedBox.shrink());
  });
}
