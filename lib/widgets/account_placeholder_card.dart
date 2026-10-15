import 'package:flutter/material.dart';

import 'app_card.dart';

/// Placeholder explaining that account features come in a later phase.
class AccountPlaceholderCard extends StatelessWidget {
  const AccountPlaceholderCard({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return AppCard(
      child: Text(
        'Sign-in and account options come in a later version. '
        'Nothing leaves your device today.',
        style: Theme.of(context)
            .textTheme
            .bodyMedium
            ?.copyWith(color: scheme.onSurfaceVariant),
      ),
    );
  }
}
