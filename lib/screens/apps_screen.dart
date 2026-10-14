import 'package:flutter/material.dart';

import '../widgets/placeholder_view.dart';

/// Placeholder for the Apps tab.
class AppsScreen extends StatelessWidget {
  const AppsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderView(
      icon: Icons.apps_outlined,
      message: 'Choose which apps show the bubble.',
    );
  }
}
