import 'package:flutter/material.dart';
import '../store/transcript_store.dart';
import '../models/transcript.dart';
import '../widgets/history_tile.dart';
import 'history_detail_screen.dart';

class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});
  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  String? _error;
  Future<void> _clear() async {
    final yes = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
                title: const Text('Delete saved dictations?'),
                content: const Text(
                    'This clears all dictations saved on this device. It cannot be undone.'),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.pop(context, false),
                      child: const Text('Keep them')),
                  FilledButton(
                      onPressed: () => Navigator.pop(context, true),
                      child: const Text('Delete all'))
                ]));
    if (yes != true) return;
    try {
      await transcripts.clear();
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Could not delete history. Try again.');
      }
    }
  }

  Future<void> _refresh() async {
    try {
      await transcripts.reload();
      if (mounted) setState(() => _error = null);
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Could not load history. Pull down to retry.');
      }
    }
  }

  @override
  Widget build(BuildContext context) =>
      ValueListenableBuilder<List<Transcript>>(
          valueListenable: transcripts,
          builder: (context, items, _) => RefreshIndicator(
              onRefresh: _refresh,
              child: ListView(
                  primary: false,
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.all(26),
                  children: [
                    Row(children: [
                      Expanded(
                          child: Text('A place for\nyour thoughts.',
                              style: Theme.of(context)
                                  .textTheme
                                  .headlineLarge
                                  ?.copyWith(
                                      letterSpacing: -1,
                                      fontWeight: FontWeight.w600))),
                      if (items.isNotEmpty)
                        IconButton(
                            tooltip: 'Delete all history',
                            onPressed: _clear,
                            icon: const Icon(Icons.delete_outline_rounded))
                    ]),
                    const SizedBox(height: 12),
                    Text('Saved on this device. Always yours.',
                        style: TextStyle(
                            color: Theme.of(context)
                                .colorScheme
                                .onSurfaceVariant)),
                    if (_error != null)
                      Padding(
                          padding: const EdgeInsets.only(top: 16),
                          child: Text(_error!)),
                    const SizedBox(height: 32),
                    if (items.isEmpty)
                      Padding(
                          padding: const EdgeInsets.symmetric(vertical: 60),
                          child: Column(children: [
                            Icon(Icons.bookmarks_outlined,
                                size: 38,
                                color: Theme.of(context).colorScheme.primary),
                            const SizedBox(height: 18),
                            const Text('A fresh start',
                                style: TextStyle(
                                    fontSize: 20, fontWeight: FontWeight.w500)),
                            const SizedBox(height: 8),
                            const Text(
                                'Save a dictation and it will live here.',
                                textAlign: TextAlign.center)
                          ])),
                    for (final item in items)
                      Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: HistoryTile(
                              item: item,
                              onTap: () => Navigator.push(
                                  context,
                                  MaterialPageRoute<void>(
                                      builder: (_) =>
                                          HistoryDetailScreen(item: item))))),
                  ])));
}
