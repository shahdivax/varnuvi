import 'package:flutter/material.dart';

/// Holds the selected theme mode: light, dark, or system (default).
/// In-memory only for now.
class ThemeController extends ValueNotifier<ThemeMode> {
  ThemeController() : super(ThemeMode.system);

  void setMode(ThemeMode mode) => value = mode;
}

/// Single app-wide instance.
final themeController = ThemeController();
