import 'package:flutter/material.dart';

import '../models/transcript.dart';
import '../utils/copy_text.dart';
import '../utils/time_format.dart';
import 'app_card.dart';

/// One history row: mode label, relative time, two-line preview, copy.
class HistoryTile extends StatelessWidget {
  const HistoryTile({super.key, required this.item, required this.onTap});

  final Transcript item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return AppCard(
      padding: const EdgeInsets.fromLTRB(20, 16, 8, 16),
      onTap: onTap,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '${item.mode.label} · ${formatRelative(item.createdAt)}',
                  style: textTheme.labelMedium
                      ?.copyWith(color: scheme.onSurfaceVariant),
                ),
                const SizedBox(height: 8),
                Text(
                  item.text,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: textTheme.bodyLarge,
                ),
              ],
            ),
          ),
          IconButton(
            tooltip: 'Copy transcript',
            icon: const Icon(Icons.copy_outlined),
            onPressed: () => copyText(context, item.text),
          ),
        ],
      ),
    );
  }
}
