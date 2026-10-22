import 'package:flutter/material.dart';

import 'app_colors.dart';

/// Light and dark Material 3 themes built from [AppColors].
/// Tonal surfaces, subtle borders, no shadows or gradients.
abstract final class AppTheme {
  static const double radius = 16;

  static ThemeData get light => _build(const ColorScheme(
        brightness: Brightness.light,
        primary: AppColors.lightAccent,
        onPrimary: AppColors.lightOnAccent,
        primaryContainer: AppColors.lightAccentContainer,
        onPrimaryContainer: AppColors.lightOnAccentContainer,
        secondary: AppColors.lightAccent,
        onSecondary: AppColors.lightOnAccent,
        secondaryContainer: AppColors.lightSurfaceHigh,
        onSecondaryContainer: AppColors.lightText,
        error: AppColors.lightError,
        onError: AppColors.lightBackground,
        surface: AppColors.lightBackground,
        onSurface: AppColors.lightText,
        onSurfaceVariant: AppColors.lightTextMuted,
        surfaceContainerLowest: AppColors.lightBackground,
        surfaceContainerLow: AppColors.lightSurface,
        surfaceContainer: AppColors.lightSurface,
        surfaceContainerHigh: AppColors.lightSurfaceHigh,
        surfaceContainerHighest: AppColors.lightSurfaceHigh,
        outline: AppColors.lightBorder,
        outlineVariant: AppColors.lightBorder,
        shadow: AppColors.lightText,
        scrim: AppColors.lightText,
        inverseSurface: AppColors.lightText,
        onInverseSurface: AppColors.lightBackground,
        inversePrimary: AppColors.darkAccent,
        surfaceTint: Colors.transparent,
      ));

  static ThemeData get dark => _build(const ColorScheme(
        brightness: Brightness.dark,
        primary: AppColors.darkAccent,
        onPrimary: AppColors.darkOnAccent,
        primaryContainer: AppColors.darkAccentContainer,
        onPrimaryContainer: AppColors.darkOnAccentContainer,
        secondary: AppColors.darkAccent,
        onSecondary: AppColors.darkOnAccent,
        secondaryContainer: AppColors.darkSurfaceHigh,
        onSecondaryContainer: AppColors.darkText,
        error: AppColors.darkError,
        onError: AppColors.darkBackground,
        surface: AppColors.darkBackground,
        onSurface: AppColors.darkText,
        onSurfaceVariant: AppColors.darkTextMuted,
        surfaceContainerLowest: AppColors.darkBackground,
        surfaceContainerLow: AppColors.darkSurface,
        surfaceContainer: AppColors.darkSurface,
        surfaceContainerHigh: AppColors.darkSurfaceHigh,
        surfaceContainerHighest: AppColors.darkSurfaceHigh,
        outline: AppColors.darkBorder,
        outlineVariant: AppColors.darkBorder,
        shadow: AppColors.darkBackground,
        scrim: AppColors.darkBackground,
        inverseSurface: AppColors.darkText,
        onInverseSurface: AppColors.darkBackground,
        inversePrimary: AppColors.lightAccent,
        surfaceTint: Colors.transparent,
      ));

  static RoundedRectangleBorder _shape() =>
      RoundedRectangleBorder(borderRadius: BorderRadius.circular(radius));

  static ThemeData _build(ColorScheme scheme) {
    OutlineInputBorder outline(Color color, [double width = 1]) =>
        OutlineInputBorder(
          borderRadius: BorderRadius.circular(radius),
          borderSide: BorderSide(color: color, width: width),
        );

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scheme.surface,
      appBarTheme: AppBarTheme(
        backgroundColor: scheme.surface,
        foregroundColor: scheme.onSurface,
        elevation: 0,
        scrolledUnderElevation: 0,
        surfaceTintColor: Colors.transparent,
      ),
      dividerTheme: DividerThemeData(color: scheme.outline, thickness: 1),
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          shape: _shape(),
          minimumSize: const Size(48, 48),
        ),
      ),
      chipTheme: ChipThemeData(
        shape: _shape(),
        side: BorderSide(color: scheme.outline),
        backgroundColor: Colors.transparent,
        selectedColor: scheme.primaryContainer,
      ),
      segmentedButtonTheme: SegmentedButtonThemeData(
        style: ButtonStyle(
          shape: WidgetStatePropertyAll(_shape()),
          side: WidgetStatePropertyAll(BorderSide(color: scheme.outline)),
          backgroundColor: WidgetStateProperty.resolveWith(
            (states) => states.contains(WidgetState.selected)
                ? scheme.primaryContainer
                : Colors.transparent,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        border: outline(scheme.outline),
        enabledBorder: outline(scheme.outline),
        focusedBorder: outline(scheme.primary, 2),
      ),
    );
  }
}
