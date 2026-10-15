import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

import '../models/installed_app.dart';

/// The only Dart <-> Kotlin entry point. Mirrors NativeBridge.kt.
abstract final class NativeBridge {
  static const _channel = MethodChannel('dev.voiceagent.app/bridge');

  static final destination = ValueNotifier<String?>(null);
  static void initialize() {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'navigate') {
        destination.value = call.arguments as String?;
      }
    });
  }

  static Future<String?> consumeDestination() =>
      _channel.invokeMethod<String>('consumeDestination');
  static Future<Map<String, dynamic>> getWritingSettings() async =>
      await _channel.invokeMapMethod<String, dynamic>('getWritingSettings') ??
      {};
  static Future<void> saveWritingDefaults(Map<String, String> defaults) =>
      _channel
          .invokeMethod<void>('saveWritingDefaults', {'defaults': defaults});
  static Future<void> saveOpenRouter(String model, String key,
          {bool clearKey = false}) =>
      _channel.invokeMethod<void>(
          'saveOpenRouter', {'model': model, 'key': key, 'clearKey': clearKey});
  static Future<List<Map<String, dynamic>>> getOpenRouterModels() async {
    final items =
        await _channel.invokeListMethod<dynamic>('getOpenRouterModels') ?? [];
    return items.map((v) => Map<String, dynamic>.from(v as Map)).toList();
  }

  static Future<void> setAiEnabled(bool enabled) =>
      _channel.invokeMethod<void>('setAiEnabled', {'enabled': enabled});

  static Future<String> testOpenRouter() async =>
      await _channel.invokeMethod<String>('testOpenRouter') ?? 'Connected';

  static Future<String?> recognizeSpeech() =>
      _channel.invokeMethod<String>('recognizeSpeech');
  static Future<String> getHistory() async =>
      await _channel.invokeMethod<String>('getHistory') ?? '[]';
  static Future<void> saveTranscript(String text) =>
      _channel.invokeMethod<void>('saveTranscript', {'text': text});
  static Future<void> clearHistory() =>
      _channel.invokeMethod<void>('clearHistory');
  static Future<String?> getTheme() =>
      _channel.invokeMethod<String>('getTheme');
  static Future<void> setTheme(String theme) =>
      _channel.invokeMethod<void>('setTheme', {'theme': theme});

  static Future<Map<String, dynamic>> getBubbleStatus() async =>
      await _channel.invokeMapMethod<String, dynamic>('getBubbleStatus') ?? {};
  static Future<bool> requestMicrophone() async =>
      await _channel.invokeMethod<bool>('requestMicrophone') ?? false;

  /// Launchable apps on the device, sorted by name.
  static Future<List<InstalledApp>> getInstalledApps() async {
    final raw = await _channel
        .invokeListMethod<Map<Object?, Object?>>('getInstalledApps');
    return [for (final m in raw ?? const []) InstalledApp.fromMap(m)];
  }

  /// Package names where the bubble is enabled.
  static Future<Set<String>> getEnabledPackages() async {
    final raw = await _channel.invokeListMethod<String>('getEnabledPackages');
    return {...?raw};
  }

  static Future<void> setPackageEnabled(String packageName, bool enabled) {
    return _channel.invokeMethod<void>(
      'setPackageEnabled',
      {'packageName': packageName, 'enabled': enabled},
    );
  }

  /// Whether the user turned on our accessibility service.
  static Future<bool> isAccessibilityServiceEnabled() async =>
      await _channel.invokeMethod<bool>('isAccessibilityServiceEnabled') ??
      false;

  /// Opens Android's accessibility settings screen.
  static Future<void> openAccessibilitySettings() =>
      _channel.invokeMethod<void>('openAccessibilitySettings');
}
