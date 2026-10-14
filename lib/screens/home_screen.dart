import 'package:flutter/material.dart';

import '../mock/mock_data.dart';
import '../models/transcript_mode.dart';
import '../widgets/mic_button.dart';
import '../widgets/mode_chips.dart';
import '../widgets/transcript_card.dart';

/// Home: mic button (idle only for now), mode chips, last transcript.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  TranscriptMode _mode = TranscriptMode.raw;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
      children: [
        Center(child: MicButton(onPressed: () {})),
        const SizedBox(height: 32),
        ModeChips(
          selected: _mode,
          onChanged: (m) => setState(() => _mode = m),
        ),
        const SizedBox(height: 32),
        TranscriptCard(
          title: 'Last transcript',
          text: kSampleTranscript,
          mode: _mode,
        ),
      ],
    );
  }
}
