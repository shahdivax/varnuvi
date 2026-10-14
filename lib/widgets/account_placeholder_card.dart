import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Placeholder explaining that account features come in a later phase.
class AccountPlaceholderCard extends StatelessWidget {
  const AccountPlaceholderCard({super.key});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppTheme.radius),
        border: Border.all(color: scheme.outline),
      ),
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
