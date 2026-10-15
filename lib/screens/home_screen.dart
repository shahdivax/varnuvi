import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../bridge/native_bridge.dart';
import '../store/transcript_store.dart';
import '../utils/copy_text.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});
  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final _text = TextEditingController();
  bool _busy = false;
  bool _saving = false;
  String? _error;
  String? _savedText;
  @override
  void dispose() {
    _text.dispose();
    super.dispose();
  }

  Future<void> _dictate() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final text = await NativeBridge.recognizeSpeech();
      if (!mounted || text == null || text.isEmpty) return;
      _text.text = text;
      _savedText = null;
    } on PlatformException catch (e) {
      if (mounted) {
        setState(() => _error =
            e.message ?? 'Could not start dictation. Please try again.');
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Dictation is available on Android.');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _save() async {
    final text = _text.text.trim();
    if (text.isEmpty || _saving) return;
    setState(() => _saving = true);
    try {
      await transcripts.save(text);
      if (!mounted) return;
      setState(() {
        _savedText = text;
        _error = null;
      });
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Saved to your history')));
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Could not save. Your text is still here.');
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _start() async {
    if (_text.text.trim().isNotEmpty && _savedText != _text.text.trim()) {
      final discard = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
                  title: const Text('Start a new dictation?'),
                  content: const Text(
                      'The current text has not been saved. Starting again will replace it.'),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('Keep text')),
                    FilledButton(
                        onPressed: () => Navigator.pop(context, true),
                        child: const Text('Start new'))
                  ]));
      if (discard != true || !mounted) return;
    }
    await _dictate();
  }

  @override
  Widget build(BuildContext context) {
    final c = Theme.of(context).colorScheme;
    final t = Theme.of(context).textTheme;
    return ListView(
        primary: false,
        padding: const EdgeInsets.fromLTRB(26, 20, 26, 32),
        children: [
          const SizedBox(height: 12),
          Text('Speak freely.',
              style: t.displaySmall?.copyWith(
                  fontSize: 45,
                  height: 1.06,
                  letterSpacing: -2,
                  fontWeight: FontWeight.w600)),
          const SizedBox(height: 16),
          const SizedBox(height: 14),
          Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                  color: c.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(28)),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(children: [
                      Icon(Icons.graphic_eq_rounded,
                          size: 20, color: c.primary),
                      const SizedBox(width: 8),
                      Expanded(child: Text('Transcript', style: t.titleSmall))
                    ]),
                    const SizedBox(height: 12),
                    TextField(
                        controller: _text,
                        onChanged: (_) => setState(() {}),
                        minLines: 5,
                        maxLines: 10,
                        style: t.bodyLarge?.copyWith(height: 1.6),
                        decoration: const InputDecoration(
                            hintText: 'Your words appear here…',
                            border: InputBorder.none,
                            enabledBorder: InputBorder.none,
                            focusedBorder: InputBorder.none,
                            contentPadding: EdgeInsets.zero),
                        textCapitalization: TextCapitalization.sentences),
                    const SizedBox(height: 12),
                    Wrap(spacing: 8, runSpacing: 8, children: [
                      FilledButton.tonalIcon(
                          onPressed: _text.text.trim().isEmpty
                              ? null
                              : () => copyText(context, _text.text.trim()),
                          icon: const Icon(Icons.copy_rounded, size: 17),
                          label: const Text('Copy')),
                      TextButton.icon(
                          onPressed: _text.text.trim().isEmpty ||
                                  _savedText == _text.text.trim() ||
                                  _saving
                              ? null
                              : _save,
                          icon: Icon(
                              _savedText == _text.text.trim() &&
                                      _savedText != null
                                  ? Icons.check_rounded
                                  : Icons.bookmark_border_rounded,
                              size: 18),
                          label: Text(_saving
                              ? 'Saving…'
                              : _savedText == _text.text.trim() &&
                                      _savedText != null
                                  ? 'Saved'
                                  : 'Save')),
                    ]),
                  ])),
          if (_error != null)
            Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Semantics(
                    liveRegion: true,
                    child: Text(_error!, style: TextStyle(color: c.error)))),
          const SizedBox(height: 24),
          FilledButton.icon(
              onPressed: _busy ? null : _start,
              style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(64),
                  shape: const StadiumBorder()),
              icon: Icon(_busy ? Icons.more_horiz_rounded : Icons.mic_rounded),
              label: Text(_busy ? 'Recording…' : 'Start dictation',
                  style: const TextStyle(
                      fontSize: 17, fontWeight: FontWeight.w600))),
          const SizedBox(height: 12),
          Text('Up to 30 minutes per session',
              textAlign: TextAlign.center,
              style: t.bodySmall?.copyWith(color: c.onSurfaceVariant)),
        ]);
  }
}
