import 'package:flutter/material.dart';

import '../bridge/native_bridge.dart';
import '../models/installed_app.dart';
import '../theme/app_spacing.dart';
import '../widgets/app_toggle_tile.dart';
import '../widgets/placeholder_view.dart';

/// Apps: the user's real installed apps, each with a bubble toggle.
/// Every app is off until the user turns it on here.
class AppsScreen extends StatefulWidget {
  const AppsScreen({super.key});

  @override
  State<AppsScreen> createState() => _AppsScreenState();
}

class _AppsScreenState extends State<AppsScreen> {
  List<InstalledApp>? _apps;
  Set<String> _enabled = {};
  String _query = '';
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final apps = await NativeBridge.getInstalledApps();
      final enabled = await NativeBridge.getEnabledPackages();
      if (!mounted) return;
      setState(() {
        _apps = apps;
        _enabled = enabled;
      });
    } catch (_) {
      if (mounted) setState(() => _failed = true);
    }
  }

  void _setLocal(String packageName, bool on) {
    setState(() {
      if (on) {
        _enabled.add(packageName);
      } else {
        _enabled.remove(packageName);
      }
    });
  }

  // Optimistic: flip the switch now, revert if the native save fails.
  Future<void> _toggle(InstalledApp app, bool on) async {
    _setLocal(app.packageName, on);
    try {
      await NativeBridge.setPackageEnabled(app.packageName, on);
    } catch (_) {
      if (!mounted) return;
      _setLocal(app.packageName, !on);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not save. Please try again.')),
      );
    }
  }

  List<InstalledApp> _visible(List<InstalledApp> apps) {
    final q = _query.trim().toLowerCase();
    if (q.isEmpty) return apps;
    return [
      for (final a in apps)
        if (a.name.toLowerCase().contains(q) ||
            a.packageName.toLowerCase().contains(q))
          a,
    ];
  }

  @override
  Widget build(BuildContext context) {
    final apps = _apps;
    if (_failed) {
      return const PlaceholderView(
        icon: Icons.error_outline,
        message: 'Could not load your apps.',
      );
    }
    if (apps == null) {
      return const Center(child: CircularProgressIndicator(
          strokeWidth: 2,
          semanticsLabel: 'Loading apps',
        ));
    }
    final visible = _visible(apps);
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.page,
            16,
            AppSpacing.page,
            8,
          ),
          child: TextField(
            onChanged: (v) => setState(() => _query = v),
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Search apps',
              prefixIcon: const Icon(Icons.search),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(
            AppSpacing.page,
            0,
            AppSpacing.page,
            8,
          ),
          child: Text(
            'The bubble appears only in apps you turn on.',
            style: Theme.of(context)
                .textTheme
                .bodySmall
                ?.copyWith(color: scheme.onSurfaceVariant),
          ),
        ),
        Expanded(
          child: visible.isEmpty
              ? PlaceholderView(
                  icon: Icons.search_off,
                  message: apps.isEmpty
                      ? 'No apps found.'
                      : 'No apps match your search.',
                )
              : ListView.builder(
                  itemCount: visible.length,
                  itemBuilder: (context, i) {
                    final app = visible[i];
                    return AppToggleTile(
                      app: app,
                      enabled: _enabled.contains(app.packageName),
                      onChanged: (on) => _toggle(app, on),
                    );
                  },
                ),
        ),
      ],
    );
  }
}
