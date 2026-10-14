import 'package:flutter/material.dart';

import '../mock/fake_dictation.dart';
import '../mock/mock_data.dart';
import '../models/mic_state.dart';
import '../models/transcript_mode.dart';
import '../widgets/mic_button.dart';
import '../widgets/mode_chips.dart';
import '../widgets/transcript_card.dart';

/// Home: mic button with fake dictation states, mode chips, last transcript.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  TranscriptMode _mode = TranscriptMode.raw;
  final _dictation = FakeDictation();

  @override
  void dispose() {
    _dictation.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(24, 16, 24, 24),
      children: [
        ValueListenableBuilder<MicState>(
          valueListenable: _dictation,
          builder: (context, state, _) =>
              _MicArea(state: state, onPressed: _dictation.toggle),
        ),
        const SizedBox(height: 24),
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

/// Mic button plus a live status line for screen readers.
class _MicArea extends StatelessWidget {
  const _MicArea({required this.state, required this.onPressed});

  final MicState state;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      children: [
        MicButton(state: state, onPressed: onPressed),
        const SizedBox(height: 8),
        Semantics(
          liveRegion: true,
          child: Text(
            state.status,
            style: Theme.of(context)
                .textTheme
                .bodyMedium
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ),
      ],
    );
  }
}
