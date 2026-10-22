import 'package:flutter_test/flutter_test.dart';
import 'package:voice_agent/utils/time_format.dart';

void main() {
  final now = DateTime(2026, 10, 15, 12, 0);

  test('relative labels', () {
    expect(formatRelative(now, now: now), 'Just now');
    expect(
        formatRelative(DateTime(2026, 10, 15, 11, 55), now: now), '5 min ago');
    expect(formatRelative(DateTime(2026, 10, 15, 9, 0), now: now), '3 h ago');
    expect(
        formatRelative(DateTime(2026, 10, 14, 11, 0), now: now), 'Yesterday');
    expect(formatRelative(DateTime(2026, 10, 10, 8, 0), now: now), '10 Oct');
  });

  test('full label pads time', () {
    expect(formatFull(DateTime(2026, 10, 12, 9, 5)), '12 Oct 2026, 09:05');
  });
}
