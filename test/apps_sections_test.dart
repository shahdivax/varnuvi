import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/screens/apps_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('enabled apps stay above other apps and move when toggled',
      (tester) async {
    const channel = MethodChannel('dev.voiceagent.app/bridge');
    final enabled = <String>{'z.app'};
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getInstalledApps') {
        return [
          {'name': 'Alpha', 'packageName': 'a.app'},
          {'name': 'Zeta', 'packageName': 'z.app'},
        ];
      }
      if (call.method == 'getEnabledPackages') return enabled.toList();
      if (call.method == 'setPackageEnabled') {
        final args = call.arguments as Map;
        if (args['enabled'] == true) {
          enabled.add(args['packageName']);
        } else {
          enabled.remove(args['packageName']);
        }
      }
      return null;
    });
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: AppsScreen())));
    await tester.pumpAndSettle();
    expect(find.text('a.app'), findsNothing);
    expect(find.text('z.app'), findsNothing);
    expect(tester.getTopLeft(find.text('Zeta')).dy,
        lessThan(tester.getTopLeft(find.text('Alpha')).dy));
    await tester.tap(find.widgetWithText(SwitchListTile, 'Alpha'));
    await tester.pumpAndSettle();
    expect(find.text('Enabled · 2'), findsOneWidget);
    await tester.enterText(find.byType(TextField), 'Zeta');
    await tester.pumpAndSettle();
    expect(find.text('Alpha'), findsNothing);
    expect(find.text('Zeta'), findsNWidgets(2));
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
  testWidgets('installed apps load only when the Apps tab is first opened',
      (tester) async {
    const channel = MethodChannel('dev.voiceagent.app/bridge');
    var loads = 0;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      if (call.method == 'getInstalledApps') {
        loads++;
        return <Map<String, String>>[];
      }
      if (call.method == 'getEnabledPackages') return <String>[];
      return null;
    });
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AppsScreen(active: false))));
    await tester.pumpAndSettle();
    expect(loads, 0);
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: AppsScreen())));
    await tester.pumpAndSettle();
    expect(loads, 1);
    await tester.pumpWidget(
        const MaterialApp(home: Scaffold(body: AppsScreen(active: false))));
    await tester.pumpAndSettle();
    await tester
        .pumpWidget(const MaterialApp(home: Scaffold(body: AppsScreen())));
    await tester.pumpAndSettle();
    expect(loads, 1);
    await tester.pumpWidget(const SizedBox.shrink());
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });
}
