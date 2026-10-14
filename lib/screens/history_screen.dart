import 'package:flutter/material.dart';

import '../mock/mock_history.dart';
import '../models/transcript.dart';
import '../widgets/history_tile.dart';
import '../widgets/placeholder_view.dart';
import 'history_detail_screen.dart';

/// History tab: mock transcripts, newest first. Tap opens the detail view.
class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  final List<Transcript> _items = mockHistory();

  void _open(Transcript item) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => HistoryDetailScreen(item: item),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_items.isEmpty) {
      return const PlaceholderView(
        icon: Icons.history,
        message: 'Your transcripts will appear here.',
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      itemCount: _items.length,
      separatorBuilder: (_, __) => const SizedBox(height: 12),
      itemBuilder: (_, i) => HistoryTile(
        item: _items[i],
        onTap: () => _open(_items[i]),
      ),
    );
  }
}
