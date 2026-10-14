import 'package:flutter/material.dart';

import '../widgets/placeholder_view.dart';

/// Placeholder for the History tab.
class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const PlaceholderView(
      icon: Icons.history,
      message: 'Your transcripts will appear here.',
    );
  }
}
