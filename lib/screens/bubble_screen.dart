import 'package:flutter/material.dart';
import '../widgets/bubble_setup_card.dart';

class BubbleScreen extends StatelessWidget {
  const BubbleScreen({super.key, this.active = true});
  final bool active;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return ListView(
      primary: false,
      padding: const EdgeInsets.fromLTRB(26, 28, 26, 32),
      children: [
        Text('Your floating mic.', style: theme.textTheme.headlineLarge),
        const SizedBox(height: 12),
        Text('A small shortcut for your words.',
            style: theme.textTheme.bodyLarge
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
        const SizedBox(height: 28),
        BubbleSetupCard(active: active),
        const SizedBox(height: 28),
        Text('Speak. Copy. Paste.', style: theme.textTheme.titleMedium),
        const SizedBox(height: 10),
        Text(
            'Drag the bubble wherever it feels comfortable. Tap to speak, '
            'review your words, then choose Copy & close. Long-press a text '
            'field in your app and tap Paste.',
            style: theme.textTheme.bodyMedium),
        const SizedBox(height: 16),
        Text(
            'The bubble stays across apps while it is on. Hide it here or '
            'from its notification whenever you need a clear screen.',
            style: theme.textTheme.bodySmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
      ],
    );
  }
}
