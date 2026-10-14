import 'package:flutter/material.dart';

import '../bridge/native_bridge.dart';
import '../theme/app_theme.dart';

/// Shows whether the accessibility service is on, explains in plain words
/// what it does and does not do, and opens Android's settings.
/// Rechecks the status whenever the user comes back to the app.
class AccessibilitySetupCard extends StatefulWidget {
  const AccessibilitySetupCard({super.key});

  @override
  State<AccessibilitySetupCard> createState() => _AccessibilitySetupCardState();
}

class _AccessibilitySetupCardState extends State<AccessibilitySetupCard>
    with WidgetsBindingObserver {
  bool? _enabled; // null while checking

  static const _does =
      'Shows a small bubble when you tap a text field in an app you turned '
      'on in the Apps tab, and puts your dictated text into that field.';
  static const _doesNot =
      'It does not read, store, or send what is on your screen. It ignores '
      'password fields and does nothing in apps you have not turned on.';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _refresh();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _refresh();
  }

  Future<void> _refresh() async {
    var enabled = false;
    try {
      enabled = await NativeBridge.isAccessibilityServiceEnabled();
    } catch (_) {
      // Treat any bridge failure as "off".
    }
    if (mounted) setState(() => _enabled = enabled);
  }

  Future<void> _openSettings() async {
    try {
      await NativeBridge.openAccessibilitySettings();
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not open accessibility settings.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final text = Theme.of(context).textTheme;
    final on = _enabled ?? false;
    final status = switch (_enabled) {
      null => 'Checking…',
      true => 'Bubble service is on',
      false => 'Bubble service is off',
    };
    final muted = text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant);
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: scheme.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppTheme.radius),
        border: Border.all(color: scheme.outline),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Semantics(
            liveRegion: true,
            child: Row(
              children: [
                Icon(
                  on ? Icons.check_circle_outline : Icons.radio_button_unchecked,
                  size: 20,
                  color: on ? scheme.primary : scheme.onSurfaceVariant,
                ),
                const SizedBox(width: 8),
                Text(status, style: text.titleSmall),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text('What it does', style: text.labelLarge),
          const SizedBox(height: 4),
          Text(_does, style: muted),
          const SizedBox(height: 12),
          Text('What it does not do', style: text.labelLarge),
          const SizedBox(height: 4),
          Text(_doesNot, style: muted),
          const SizedBox(height: 20),
          FilledButton.tonal(
            onPressed: _openSettings,
            child: Text(on ? 'Manage in Android settings' : 'Turn on in Android settings'),
          ),
        ],
      ),
    );
  }
}
