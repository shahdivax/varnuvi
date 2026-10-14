import 'package:flutter/material.dart';

import '../widgets/placeholder_view.dart';

/// Placeholder for the Home tab.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderView(
      icon: Icons.mic_none_outlined,
      message: 'Home comes next.',
    );
  }
}
