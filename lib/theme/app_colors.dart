import 'package:flutter/painting.dart';

/// Every app color lives here. No pure white or pure black anywhere.
/// Accent: one muted, desaturated dusty slate blue.
abstract final class AppColors {
  // Light: ivory and warm off-white, dark charcoal text.
  static const lightBackground = Color(0xFFF7F3EA);
  static const lightSurface = Color(0xFFF0EADD);
  static const lightSurfaceHigh = Color(0xFFE9E2D3);
  static const lightBorder = Color(0xFFDAD1BF);
  static const lightText = Color(0xFF2B2A27);
  static const lightTextMuted = Color(0xFF6B675F);
  static const lightAccent = Color(0xFF5E6B7D);
  static const lightOnAccent = Color(0xFFF7F3EA);
  static const lightAccentContainer = Color(0xFFDCDFE3);
  static const lightOnAccentContainer = Color(0xFF2E3744);
  static const lightError = Color(0xFF9A4F4F);

  // Dark: layered charcoal, soft off-white text.
  static const darkBackground = Color(0xFF1A1B1E);
  static const darkSurface = Color(0xFF232428);
  static const darkSurfaceHigh = Color(0xFF2C2D32);
  static const darkBorder = Color(0xFF3A3B41);
  static const darkText = Color(0xFFE8E4DA);
  static const darkTextMuted = Color(0xFFA8A49B);
  static const darkAccent = Color(0xFF9AA7B8);
  static const darkOnAccent = Color(0xFF1F242B);
  static const darkAccentContainer = Color(0xFF3A4350);
  static const darkOnAccentContainer = Color(0xFFDCDFE3);
  static const darkError = Color(0xFFD29C9C);
}
