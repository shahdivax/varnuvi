import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../bridge/native_bridge.dart';

class OpenRouterScreen extends StatefulWidget {
  const OpenRouterScreen({super.key});
  @override
  State<OpenRouterScreen> createState() => _OpenRouterScreenState();
}

class _OpenRouterScreenState extends State<OpenRouterScreen> {
  final _key = TextEditingController();
  final _model = TextEditingController();
  bool get _connectionReady =>
      _tested && _key.text.isEmpty && _model.text.trim() == _savedModel;
  bool _hasKey = false;
  bool _tested = false;
  bool _enabled = false;
  String _savedModel = '';
  bool _loading = true;
  bool _busy = false;
  bool _visible = false;
  String? _message;
  bool _success = false;
  List<Map<String, dynamic>>? _models;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final state = await NativeBridge.getWritingSettings();
      if (mounted) {
        setState(() {
          _hasKey = state['hasKey'] == true;
          _tested = state['tested'] == true;
          _enabled = state['aiEnabled'] == true;
          _savedModel = state['model'] as String? ?? '';
          _model.text = state['model'] as String? ?? '';
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _loading = false;
          _message = 'Could not load AI settings.';
        });
      }
    }
  }

  @override
  void dispose() {
    _key.dispose();
    _model.dispose();
    super.dispose();
  }

  String _failure(Object e) => e is PlatformException
      ? e.message ?? 'OpenRouter request failed.'
      : 'Could not connect. Please try again.';
  Future<void> _save({bool test = false, bool remove = false}) async {
    if (_busy) return;
    if (!remove &&
        (_model.text.trim().isEmpty ||
            (!_hasKey && _key.text.trim().isEmpty))) {
      setState(() {
        _message = 'Enter an API key and choose a model.';
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
      final entered = _key.text.trim().isNotEmpty;
      if (!(test && _connectionReady)) {
        await NativeBridge.saveOpenRouter(_model.text.trim(), _key.text.trim(),
            clearKey: remove);
      }
      if (!mounted) return;
      setState(() {
        _hasKey = !remove && (_hasKey || entered);
        _key.clear();
      });
      final response = test
          ? await NativeBridge.testOpenRouter()
          : remove
              ? 'Saved API key removed.'
              : 'AI settings saved.';
      if (mounted) {
        setState(() {
          _message = response;
          _success = true;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _message = _failure(e);
          _success = false;
        });
      }
    } finally {
      await _sync();
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _sync() async {
    try {
      final state = await NativeBridge.getWritingSettings();
      if (mounted) {
        setState(() {
          _hasKey = state['hasKey'] == true;
          _tested = state['tested'] == true;
          _enabled = state['aiEnabled'] == true;
          _savedModel = state['model'] as String? ?? '';
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _tested = false;
          _enabled = false;
        });
      }
    }
  }

  Future<void> _toggle(bool on) async {
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      await NativeBridge.setAiEnabled(on);
      await _sync();
    } catch (e) {
      if (mounted) {
        setState(() {
          _message = _failure(e);
          _success = false;
        });
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _browse() async {
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      _models ??= await NativeBridge.getOpenRouterModels();
      if (!mounted) return;
      final selected = await showModalBottomSheet<String>(
          context: context,
          isScrollControlled: true,
          useSafeArea: true,
          builder: (_) => _ModelPicker(models: _models!));
      if (selected != null && mounted) {
        setState(() {
          _model.text = selected;
          _message = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _message = _failure(e);
          _success = false;
        });
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
      appBar: AppBar(title: const Text('AI')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView(padding: const EdgeInsets.all(24), children: [
              Text('Choose your writing model.',
                  style: Theme.of(context)
                      .textTheme
                      .headlineMedium
                      ?.copyWith(letterSpacing: -1)),
              const SizedBox(height: 12),
              const Text(
                  'Your key is encrypted on this device. Connect through OpenRouter, test your model, then choose whether to use AI. AI starts off.'),
              const SizedBox(height: 20),
              if (_connectionReady)
                SwitchListTile.adaptive(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Use AI'),
                  subtitle: Text(_enabled
                      ? 'Follows your spoken instructions and saved preferences, even when all defaults are None.'
                      : 'Transcription only. No transcript is sent to AI; saved preferences stay available.'),
                  value: _enabled,
                  onChanged: _busy ? null : _toggle,
                )
              else
                const Text(
                    'Save and test your connection to unlock the Use AI switch.'),
              const SizedBox(height: 28),
              TextField(
                  controller: _key,
                  onChanged: (_) => setState(() {}),
                  obscureText: !_visible,
                  enableSuggestions: false,
                  autocorrect: false,
                  enabled: !_busy,
                  decoration: InputDecoration(
                      labelText: 'API key',
                      hintText: _hasKey
                          ? 'Key saved · leave blank to keep it'
                          : 'Paste your OpenRouter key',
                      suffixIcon: IconButton(
                          tooltip: _visible ? 'Hide key' : 'Show key',
                          onPressed: () => setState(() => _visible = !_visible),
                          icon: Icon(_visible
                              ? Icons.visibility_off_outlined
                              : Icons.visibility_outlined)))),
              const SizedBox(height: 22),
              TextField(
                  controller: _model,
                  onChanged: (_) => setState(() {}),
                  enabled: !_busy,
                  autocorrect: false,
                  decoration: const InputDecoration(
                      labelText: 'Model ID',
                      hintText: 'Choose a model or enter its ID')),
              Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton.icon(
                      onPressed: _busy ? null : _browse,
                      icon: const Icon(Icons.search_rounded),
                      label: const Text('Search models'))),
              const SizedBox(height: 16),
              FilledButton.icon(
                  onPressed: _busy ? null : () => _save(test: true),
                  icon: const Icon(Icons.check_circle_outline),
                  label: Text(_busy
                      ? 'Working…'
                      : _connectionReady
                          ? 'Retest connection'
                          : 'Save & test connection')),
              if (!_connectionReady) ...[
                const SizedBox(height: 10),
                TextButton(
                    onPressed: _busy ? null : () => _save(),
                    child: const Text('Save without testing')),
              ],
              if (_hasKey)
                PopupMenuButton<String>(
                  enabled: !_busy,
                  tooltip: 'Connection options',
                  onSelected: (_) => _save(remove: true),
                  itemBuilder: (_) => [
                    const PopupMenuItem(
                        value: 'remove', child: Text('Remove saved key'))
                  ],
                  child: const Padding(
                      padding: EdgeInsets.all(12),
                      child: Text('Connection options',
                          textAlign: TextAlign.center)),
                ),
              if (_message != null)
                Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Semantics(
                        liveRegion: true,
                        child: Text(_message!,
                            style: TextStyle(
                                color: _success
                                    ? Theme.of(context).colorScheme.primary
                                    : Theme.of(context).colorScheme.error)))),
              const SizedBox(height: 16),
              const Text(
                  'Usage is billed to your OpenRouter key. The connection test summarizes a fixed example into two bullet points and shows the result. Use a model with enough context for long recordings.'),
            ]));
}

class _ModelPicker extends StatefulWidget {
  const _ModelPicker({required this.models});
  final List<Map<String, dynamic>> models;
  @override
  State<_ModelPicker> createState() => _ModelPickerState();
}

class _ModelPickerState extends State<_ModelPicker> {
  String _query = '';
  @override
  Widget build(BuildContext context) {
    final models = widget.models
        .where((m) => '${m['name']} ${m['id']}'
            .toLowerCase()
            .contains(_query.toLowerCase()))
        .toList();
    return SizedBox(
        height: MediaQuery.sizeOf(context).height * .85,
        child: Column(children: [
          Padding(
              padding: const EdgeInsets.all(20),
              child: TextField(
                  autofocus: true,
                  onChanged: (v) => setState(() => _query = v),
                  decoration: const InputDecoration(
                      labelText: 'Search models',
                      prefixIcon: Icon(Icons.search)))),
          Expanded(
              child: models.isEmpty
                  ? const Center(child: Text('No matching text models.'))
                  : ListView.builder(
                      itemCount: models.length,
                      itemBuilder: (context, i) {
                        final m = models[i];
                        return ListTile(
                            title: Text(m['name'] as String),
                            subtitle: Text(
                                '${m['id']}\n${m['context']} token context'),
                            isThreeLine: true,
                            onTap: () =>
                                Navigator.pop(context, m['id'] as String));
                      })),
        ]));
  }
}
