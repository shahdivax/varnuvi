import 'package:flutter/material.dart';

import '../models/transcript.dart';
import '../utils/copy_text.dart';
import '../utils/time_format.dart';

/// Full view of one transcript. Primary action: copy.
class HistoryDetailScreen extends StatelessWidget {
  const HistoryDetailScreen({super.key, required this.item});

  final Transcript item;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final textTheme = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: Text(item.mode.label)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
          children: [
            Text(
              formatFull(item.createdAt),
              style:
                  textTheme.labelLarge?.copyWith(color: scheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            SelectableText(
              item.text,
              style: textTheme.bodyLarge?.copyWith(height: 1.5),
            ),
            const SizedBox(height: 32),
            Align(
              alignment: Alignment.centerLeft,
              child: FilledButton.tonalIcon(
                onPressed: () => copyText(context, item.text),
                icon: const Icon(Icons.copy_outlined),
                label: const Text('Copy'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
