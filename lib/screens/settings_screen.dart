import 'package:flutter/material.dart';

import '../widgets/placeholder_view.dart';

/// Placeholder for the Settings tab.
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderView(
      icon: Icons.tune,
      message: 'Settings come later.',
    );
  }
}
