import 'package:flutter/material.dart';

import '../models/transcript_mode.dart';
import '../theme/app_theme.dart';

/// Tonal card showing a transcript and its mode label.
class TranscriptCard extends StatelessWidget {
  const TranscriptCard({
    super.key,
    required this.title,
    required this.text,
    required this.mode,
  });

  final String title;
  final String text;
  final TranscriptMode mode;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppTheme.radius),
        border: Border.all(color: scheme.outline),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: textTheme.labelLarge
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
              ),
              Text(
                mode.label,
                style: textTheme.labelMedium?.copyWith(color: scheme.primary),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(text, style: textTheme.bodyLarge),
        ],
      ),
    );
  }
}
