import 'package:flutter/material.dart';
import '../bridge/native_bridge.dart';

class ThemeController extends ValueNotifier<ThemeMode> {
  ThemeController() : super(ThemeMode.system);
  Future<void> load() async {
    final name = await NativeBridge.getTheme();
    value = ThemeMode.values
        .firstWhere((m) => m.name == name, orElse: () => ThemeMode.system);
  }

  Future<void> setMode(ThemeMode mode) async {
    await NativeBridge.setTheme(mode.name);
    value = mode;
  }
}

final themeController = ThemeController();
