import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../bridge/native_bridge.dart';
import 'openrouter_screen.dart';

const writingDefaultOptions = <String, Map<String, String>>{
  'action': {
    'none': 'None · follow input',
    'translate': 'Translate',
    'summary': 'Summarize',
    'paraphrase': 'Paraphrase'
  },
  'inputLanguage': {
    'auto': 'None · detect automatically',
    'en': 'English',
    'gu': 'Gujarati',
    'hi': 'Hindi',
    'mr': 'Marathi',
    'ta': 'Tamil',
    'te': 'Telugu',
    'bn': 'Bengali',
    'es': 'Spanish',
    'fr': 'French',
    'de': 'German',
    'ja': 'Japanese'
  },
  'outputLanguage': {
    'auto': 'None · follow input',
    'English': 'English',
    'Gujarati': 'Gujarati',
    'Hindi': 'Hindi',
    'Marathi': 'Marathi',
    'Tamil': 'Tamil',
    'Telugu': 'Telugu',
    'Bengali': 'Bengali',
    'Spanish': 'Spanish',
    'French': 'French',
    'German': 'German',
    'Japanese': 'Japanese'
  },
  'tone': {
    'none': 'None · follow input',
    'professional': 'Professional',
    'friendly': 'Friendly',
    'formal': 'Formal',
    'casual': 'Casual',
    'direct': 'Direct'
  },
  'style': {
    'none': 'None · follow input',
    'paragraphs': 'Paragraphs',
    'bullet points': 'Bullet points',
    'email': 'Email',
    'short message': 'Short message'
  },
};
Map<String, String> emptyWritingDefaults() => {
      for (final field in writingDefaultOptions.keys)
        field: field.endsWith('Language') ? 'auto' : 'none'
    };

class WritingDefaultsScreen extends StatefulWidget {
  const WritingDefaultsScreen({super.key});
  @override
  State<WritingDefaultsScreen> createState() => _WritingDefaultsScreenState();
}

class _WritingDefaultsScreenState extends State<WritingDefaultsScreen> {
  Map<String, String> _values = emptyWritingDefaults();
  Map<String, String> _saved = emptyWritingDefaults();
  bool _loading = true;
  bool _saving = false;
  bool _connected = false;
  String? _error;
  bool get _dirty => !mapEquals(_values, _saved);
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final state = await NativeBridge.getWritingSettings();
      if (!mounted) return;
      final raw = state['defaults'] as Map? ?? {};
      setState(() {
        _values = {
          for (final field in writingDefaultOptions.keys)
            field: writingDefaultOptions[field]!.containsKey(raw[field])
                ? raw[field] as String
                : emptyWritingDefaults()[field]!
        };
        _saved = Map.of(_values);
        _connected = state['hasKey'] == true &&
            (state['model'] as String? ?? '').isNotEmpty;
        _loading = false;
        _error = null;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = 'Could not load writing defaults. Try again.';
        });
      }
    }
  }

  Future<void> _save() async {
    if (_saving) return;
    setState(() => _saving = true);
    try {
      await NativeBridge.saveWritingDefaults(_values);
      if (!mounted) return;
      setState(() {
        _saved = Map.of(_values);
        _error = null;
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text('Defaults saved for your next recording')));
    } on PlatformException catch (e) {
      if (mounted) {
        setState(() => _error = e.message ?? 'Could not save defaults.');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
        canPop: !_dirty || _saving,
        onPopInvokedWithResult: (didPop, _) async {
          if (didPop) return;
          final discard = await showDialog<bool>(
              context: context,
              builder: (context) => AlertDialog(
                      title: const Text('Discard unsaved defaults?'),
                      actions: [
                        TextButton(
                            onPressed: () => Navigator.pop(context, false),
                            child: const Text('Keep editing')),
                        FilledButton(
                            onPressed: () => Navigator.pop(context, true),
                            child: const Text('Discard'))
                      ]));
          if (discard == true && context.mounted) Navigator.pop(context);
        },
        child: Scaffold(
            appBar: AppBar(title: const Text('Writing defaults'), actions: [
              TextButton(
                  onPressed: _loading || _saving
                      ? null
                      : () => setState(() => _values = emptyWritingDefaults()),
                  child: const Text('Reset'))
            ]),
            body: _loading
                ? const Center(child: CircularProgressIndicator())
                : ListView(padding: const EdgeInsets.all(24), children: [
                    Text('Your words. Your way.',
                        style: Theme.of(context)
                            .textTheme
                            .headlineMedium
                            ?.copyWith(letterSpacing: -1)),
                    const SizedBox(height: 12),
                    const Text(
                        'Leave None to follow your input. Saved choices are gentle defaults; explicit writing instructions in your dictation take priority when Use AI is on.'),
                    const SizedBox(height: 22),
                    if (!_connected)
                      Padding(
                          padding: const EdgeInsets.only(bottom: 22),
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                    'Connect and turn on AI to apply translation, summaries, tone, and style. Spoken language works without AI.'),
                                TextButton.icon(
                                    onPressed: () async {
                                      await Navigator.push(
                                          context,
                                          MaterialPageRoute<void>(
                                              builder: (_) =>
                                                  const OpenRouterScreen()));
                                      if (!mounted) return;
                                      try {
                                        final state = await NativeBridge
                                            .getWritingSettings();
                                        if (mounted) {
                                          setState(() => _connected =
                                              state['hasKey'] == true &&
                                                  (state['model'] as String? ??
                                                          '')
                                                      .isNotEmpty);
                                        }
                                      } catch (_) {
                                        /* Leave the connection hint visible. */
                                      }
                                    },
                                    icon: const Icon(Icons.link),
                                    label: const Text('Set up AI'))
                              ])),
                    for (final field in writingDefaultOptions.keys)
                      Padding(
                          padding: const EdgeInsets.only(bottom: 22),
                          child: DropdownButtonFormField<String>(
                            key: ValueKey('$field-${_values[field]}'),
                            initialValue: _values[field],
                            isExpanded: true,
                            decoration: InputDecoration(
                                labelText: const {
                              'action': 'Default action',
                              'inputLanguage': 'Spoken language',
                              'outputLanguage': 'Output language',
                              'tone': 'Tone',
                              'style': 'Style'
                            }[field]),
                            items: [
                              for (final option
                                  in writingDefaultOptions[field]!.entries)
                                DropdownMenuItem(
                                    value: option.key,
                                    child: Text(option.value))
                            ],
                            onChanged: _saving
                                ? null
                                : (v) {
                                    if (v != null) {
                                      setState(() => _values[field] = v);
                                    }
                                  },
                          )),
                    if (_error != null)
                      Padding(
                          padding: const EdgeInsets.only(bottom: 16),
                          child: Text(_error!,
                              style: TextStyle(
                                  color: Theme.of(context).colorScheme.error))),
                    FilledButton(
                        onPressed: _saving || !_dirty ? null : _save,
                        child: Text(_saving ? 'Saving…' : 'Save defaults')),
                    const SizedBox(height: 16),
                    const Text(
                        'When Use AI is on, AI follows instructions in your dictation even when every default is None. When off, only transcription runs. Model responses can vary.'),
                  ])),
      );
}
