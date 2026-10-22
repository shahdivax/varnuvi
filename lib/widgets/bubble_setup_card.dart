import 'dart:async';
import 'package:flutter/material.dart';
import '../bridge/native_bridge.dart';
import 'app_card.dart';

/// Reads live native service state, rather than assuming permission means ready.
class BubbleSetupCard extends StatefulWidget {
  const BubbleSetupCard({super.key, this.active = true});
  final bool active;
  @override
  State<BubbleSetupCard> createState() => _BubbleSetupCardState();
}

class _BubbleSetupCardState extends State<BubbleSetupCard>
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
  void didUpdateWidget(BubbleSetupCard oldWidget) {
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
      if (s['connected'] == true) {
        await NativeBridge.setBubbleEnabled(false);
      } else if (s['microphone'] != true) {
        await NativeBridge.requestMicrophone();
      } else if (s['enabled'] != true) {
        await NativeBridge.openOverlaySettings();
      } else {
        await NativeBridge.setBubbleEnabled(s['connected'] != true);
      }
      await _refresh();
    } catch (_) {
      if (mounted) {
        setState(() => _actionError =
            'Could not update the bubble. Check Display over other apps in Android settings.');
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
    final ready = mic && enabled && connected;
    final visible = s?['visible'] == true;
    final dictating = s?['dictating'] == true;
    final label = s == null
        ? 'Checking bubble…'
        : connected
            ? 'Hide bubble'
            : !mic
                ? 'Allow microphone'
                : !enabled
                    ? 'Allow floating bubble'
                    : 'Show bubble';
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
                            ? 'Bubble hidden'
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
      _step('Display over other apps', enabled),
      const SizedBox(height: 14),
      Text(
          ready
              ? 'Tap the floating mic to dictate. Review, copy, then paste into your app.'
              : mic && !enabled
                  ? 'Allow Varnuvi to display over other apps. Accessibility access is not needed.'
                  : 'Your mic stays off until you tap the bubble and start dictating.',
          style: Theme.of(context).textTheme.bodySmall),
      const SizedBox(height: 18),
      FilledButton(
          onPressed: s == null || _opening ? null : _setup,
          child: Text(_opening ? 'Opening…' : label)),
    ]));
  }
}
