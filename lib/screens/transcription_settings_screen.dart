import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../bridge/native_bridge.dart';
import '../widgets/app_card.dart';

class TranscriptionSettingsScreen extends StatefulWidget {
  const TranscriptionSettingsScreen({super.key});
  @override
  State<TranscriptionSettingsScreen> createState() =>
      _TranscriptionSettingsScreenState();
}

class _TranscriptionSettingsScreenState
    extends State<TranscriptionSettingsScreen> {
  final _url = TextEditingController();
  final _token = TextEditingController();
  bool _loading = true;
  bool _busy = false;
  bool _hasToken = false;
  bool _success = false;
  String _savedUrl = '';
  String? _message;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final state = await NativeBridge.getTranscriptionSettings();
      if (!mounted) return;
      setState(() {
        _savedUrl = state['endpoint'] as String? ?? '';
        _url.text = _savedUrl;
        _hasToken = state['hasToken'] == true;
      });
    } catch (_) {
      if (mounted) {
        setState(() => _message = 'Could not load your server connection.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  void dispose() {
    _url.dispose();
    _token.dispose();
    super.dispose();
  }

  void _edited(String _) => setState(() {
        _message = null;
        _success = false;
      });
  Future<void> _save() async {
    if (_busy) return;
    if (_url.text.trim().isEmpty ||
        (_token.text.isEmpty &&
            (!_hasToken || _url.text.trim() != _savedUrl))) {
      setState(() {
        _message = 'Enter your server URL and its access token.';
        _success = false;
      });
      return;
    }
    setState(() {
      _busy = true;
      _message = null;
      _success = false;
    });
    try {
      await NativeBridge.saveTranscriptionSettings(
          _url.text.trim(), _token.text);
      if (!mounted) return;
      setState(() {
        _savedUrl = _url.text.trim();
        _hasToken = true;
        _token.clear();
      });
      final message = await NativeBridge.testTranscriptionServer();
      if (mounted) {
        setState(() {
          _message = message;
          _success = true;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() => _message = error is PlatformException
            ? error.message ?? 'Could not connect to your server.'
            : 'Could not connect to your server.');
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _remove() async {
    final confirmed = await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
              title: const Text('Remove server connection?'),
              content: const Text(
                  'Your saved transcripts will stay. Set up a server again before your next dictation.'),
              actions: [
                TextButton(
                    onPressed: () => Navigator.pop(context, false),
                    child: const Text('Keep connection')),
                FilledButton(
                    onPressed: () => Navigator.pop(context, true),
                    child: const Text('Remove'))
              ],
            ));
    if (confirmed != true || !mounted) return;
    setState(() => _busy = true);
    try {
      await NativeBridge.saveTranscriptionSettings('', '', clear: true);
      if (mounted) {
        setState(() {
          _url.clear();
          _token.clear();
          _savedUrl = '';
          _hasToken = false;
          _success = false;
          _message = 'Server connection removed.';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _success = false;
          _message = 'Could not remove the connection. Try again.';
        });
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text('Speech server')),
      body: _loading
          ? const Center(
              child: CircularProgressIndicator(
                  semanticsLabel: 'Loading server settings'))
          : ListView(padding: const EdgeInsets.all(26), children: [
              Text('Your voice. Your server.',
                  style: theme.textTheme.headlineSmall),
              const SizedBox(height: 12),
              Text(
                  'Connect the transcription API you host on your PC or cloud server. No shared server is included.',
                  style: theme.textTheme.bodyMedium),
              const SizedBox(height: 24),
              AppCard(
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                    TextField(
                      controller: _url,
                      enabled: !_busy,
                      onChanged: _edited,
                      keyboardType: TextInputType.url,
                      autocorrect: false,
                      decoration: const InputDecoration(
                          labelText: 'API URL',
                          hintText: 'https://x.xx.xxx/api/transcribe',
                          helperText: 'Use the complete /api/transcribe URL.'),
                    ),
                    const SizedBox(height: 20),
                    TextField(
                      controller: _token,
                      enabled: !_busy,
                      onChanged: _edited,
                      obscureText: true,
                      autocorrect: false,
                      enableSuggestions: false,
                      decoration: InputDecoration(
                          labelText: 'Access token',
                          hintText: _hasToken
                              ? 'Saved securely · enter to replace'
                              : 'Token from your server configuration'),
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                        onPressed: _busy ? null : _save,
                        child: Text(
                            _busy ? 'Connecting…' : 'Save & test connection')),
                    if (_message != null) ...[
                      const SizedBox(height: 16),
                      Semantics(
                          liveRegion: true,
                          child: Text(_message!,
                              style: TextStyle(
                                  color: _success
                                      ? theme.colorScheme.primary
                                      : theme.colorScheme.onSurfaceVariant))),
                    ],
                  ])),
              const SizedBox(height: 20),
              Text(
                  'Public servers require HTTPS. For a local PC, use HTTPS or USB forwarding to localhost. See the self-hosting guide in the repository.',
                  style: theme.textTheme.bodySmall),
              if (_savedUrl.isNotEmpty)
                Padding(
                    padding: const EdgeInsets.only(top: 16),
                    child: TextButton(
                        onPressed: _busy ? null : _remove,
                        child: const Text('Remove connection'))),
            ]),
    );
  }
}
