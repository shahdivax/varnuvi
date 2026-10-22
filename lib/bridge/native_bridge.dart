import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

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
  static Future<Map<String, dynamic>> getTranscriptionSettings() async =>
      await _channel
          .invokeMapMethod<String, dynamic>('getTranscriptionSettings') ??
      {};
  static Future<void> saveTranscriptionSettings(String endpoint, String token,
          {bool clear = false}) =>
      _channel.invokeMethod<void>('saveTranscriptionSettings',
          {'endpoint': endpoint, 'token': token, 'clear': clear});
  static Future<String> testTranscriptionServer() async =>
      await _channel.invokeMethod<String>('testTranscriptionServer') ??
      'Server connected.';

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

  static Future<void> openOverlaySettings() =>
      _channel.invokeMethod<void>('openOverlaySettings');

  static Future<void> setBubbleEnabled(bool enabled) =>
      _channel.invokeMethod<void>('setBubbleEnabled', {'enabled': enabled});
}
