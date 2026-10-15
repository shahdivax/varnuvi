/// Package-free timestamp formatting (no intl dependency).
library;

const _months = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', //
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

/// Relative label for lists: "Just now", "5 min ago", "2 h ago",
/// "Yesterday", or a short date like "12 Oct".
String formatRelative(DateTime t, {DateTime? now}) {
  final diff = (now ?? DateTime.now()).difference(t);
  if (diff.inMinutes < 1) return 'Just now';
  if (diff.inHours < 1) return '${diff.inMinutes} min ago';
  if (diff.inDays < 1) return '${diff.inHours} h ago';
  if (diff.inDays == 1) return 'Yesterday';
  return '${t.day} ${_months[t.month - 1]}';
}

/// Full label for the detail view, e.g. "12 Oct 2026, 09:05".
String formatFull(DateTime t) {
  String two(int n) => n.toString().padLeft(2, '0');
  return '${t.day} ${_months[t.month - 1]} ${t.year}, '
      '${two(t.hour)}:${two(t.minute)}';
}
