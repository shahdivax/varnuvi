import 'package:flutter/services.dart';

import '../models/installed_app.dart';

/// The only Dart <-> Kotlin entry point. Mirrors NativeBridge.kt.
abstract final class NativeBridge {
  static const _channel = MethodChannel('dev.voiceagent.app/bridge');

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
}
