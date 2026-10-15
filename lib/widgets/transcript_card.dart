import 'package:flutter/material.dart';

import '../models/transcript_mode.dart';
import 'app_card.dart';

/// Card showing a transcript and its mode label.
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
    return AppCard(
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
