import 'dart:async';
import 'package:flutter/material.dart';
import '../bridge/native_bridge.dart';
import '../screens/apps_screen.dart';
import 'app_card.dart';

/// Reads live native service state, rather than assuming permission means ready.
class AccessibilitySetupCard extends StatefulWidget {
  const AccessibilitySetupCard({super.key, this.active = true});
  final bool active;
  @override
  State<AccessibilitySetupCard> createState() => _AccessibilitySetupCardState();
}

class _AccessibilitySetupCardState extends State<AccessibilitySetupCard>
    with WidgetsBindingObserver {
  Map<String, dynamic>? _status;
  String? _statusError;
  String? _actionError;
  Timer? _poll;
  bool _loading = false;
  bool _opening = false;
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.active) {
      _refresh();
      _startPolling();
    }
  }

  @override
  void didUpdateWidget(AccessibilitySetupCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active == oldWidget.active) return;
    _poll?.cancel();
    if (widget.active) {
      _refresh();
      _startPolling();
    }
  }

  void _startPolling() {
    _poll?.cancel();
    _poll = Timer.periodic(const Duration(seconds: 2), (_) => _refresh());
  }

  @override
  void dispose() {
    _poll?.cancel();
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.active) {
      _refresh();
      _startPolling();
    } else {
      _poll?.cancel();
    }
  }

  Future<void> _refresh() async {
    if (_loading) return;
    _loading = true;
    try {
      final status = await NativeBridge.getBubbleStatus();
      if (mounted) {
        setState(() {
          _status = status;
          _statusError = null;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() => _statusError =
            'Could not check bubble status. Tap refresh to retry.');
      }
    } finally {
      _loading = false;
    }
  }

  Future<void> _setup() async {
    if (_opening) return;
    setState(() {
      _opening = true;
      _actionError = null;
    });
    try {
      final s = await NativeBridge.getBubbleStatus();
      if (!mounted) return;
      if (s['microphone'] != true) {
        await NativeBridge.requestMicrophone();
      } else if (s['enabled'] != true || s['connected'] != true) {
        await NativeBridge.openAccessibilitySettings();
      } else {
        await Navigator.push(
            context,
            MaterialPageRoute<void>(
                builder: (_) => Scaffold(
                    appBar: AppBar(title: const Text('Choose apps')),
                    body: const SafeArea(child: AppsScreen()))));
      }
      await _refresh();
    } catch (_) {
      if (mounted) {
        setState(() => _actionError =
            'Could not open Android settings. Open Settings → Accessibility and select Varnuvi.');
      }
    } finally {
      if (mounted) setState(() => _opening = false);
    }
  }

  Widget _step(String title, bool done) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 7),
      child: Row(children: [
        Icon(done ? Icons.check_circle_rounded : Icons.radio_button_unchecked,
            color: done
                ? Theme.of(context).colorScheme.primary
                : Theme.of(context).colorScheme.onSurfaceVariant,
            size: 20),
        const SizedBox(width: 10),
        Expanded(child: Text(title))
      ]));
  @override
  Widget build(BuildContext context) {
    final s = _status;
    final mic = s?['microphone'] == true;
    final enabled = s?['enabled'] == true;
    final connected = s?['connected'] == true;
    final count = (s?['appCount'] as num?)?.toInt() ?? 0;
    final ready = mic && enabled && connected && count > 0;
    final visible = s?['visible'] == true;
    final dictating = s?['dictating'] == true;
    final label = s == null
        ? 'Checking bubble…'
        : !mic
            ? 'Allow microphone'
            : !enabled
                ? 'Enable bubble'
                : !connected
                    ? 'Reconnect bubble'
                    : count == 0
                        ? 'Choose apps'
                        : 'Manage apps';
    final status = _statusError != null
        ? 'Status unavailable'
        : s == null
            ? 'Checking…'
            : dictating
                ? 'Dictation in progress'
                : visible
                    ? 'Bubble visible'
                    : ready
                        ? 'Bubble ready'
                        : enabled && !connected
                            ? 'Service disconnected'
                            : 'Set up your bubble';
    return AppCard(
        child:
            Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Row(children: [
        Expanded(
            child: Semantics(
                liveRegion: true,
                child: Text(status,
                    style: Theme.of(context).textTheme.titleMedium))),
        IconButton(
            tooltip: 'Refresh bubble status',
            onPressed: _refresh,
            icon: const Icon(Icons.refresh_rounded))
      ]),
      if (_statusError != null || _actionError != null)
        Text(_statusError ?? _actionError!,
            style: TextStyle(color: Theme.of(context).colorScheme.error)),
      const SizedBox(height: 8),
      _step('Microphone access', mic),
      _step('Accessibility service', enabled && connected),
      _step(
          count == 0
              ? 'Choose where it appears'
              : '$count ${count == 1 ? 'app' : 'apps'} enabled',
          count > 0),
      const SizedBox(height: 14),
      Text(
          ready
              ? 'Open an enabled app to show the bubble. Select a text field before dictating.'
              : mic && !enabled
                  ? 'In Android accessibility settings, select Varnuvi and turn it on.'
                  : 'Android will ask you to approve access. Varnuvi skips password fields.',
          style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: 18),
      FilledButton(
          onPressed: s == null || _opening ? null : _setup,
          child: Text(_opening ? 'Opening…' : label)),
      if (enabled)
        TextButton(
            onPressed: () async {
              try {
                await NativeBridge.openAccessibilitySettings();
              } catch (_) {
                if (mounted) {
                  setState(() => _actionError =
                      'Could not open Android settings. Open Settings → Accessibility and select Varnuvi.');
                }
              }
            },
            child: const Text('Manage service')),
    ]));
  }
}
