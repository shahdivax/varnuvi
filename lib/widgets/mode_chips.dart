import 'package:flutter/material.dart';

import '../models/transcript_mode.dart';

/// Single-select chips for picking the transcript mode.
class ModeChips extends StatelessWidget {
  const ModeChips({super.key, required this.selected, required this.onChanged});

  final TranscriptMode selected;
  final ValueChanged<TranscriptMode> onChanged;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      spacing: 8,
      runSpacing: 8,
      children: [
        for (final mode in TranscriptMode.values)
          ChoiceChip(
            label: Text(mode.label),
            selected: mode == selected,
            showCheckmark: false,
            onSelected: (_) => onChanged(mode),
          ),
      ],
    );
  }
}
