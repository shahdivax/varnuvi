import 'package:flutter/material.dart';

import '../bridge/native_bridge.dart';
import '../models/installed_app.dart';
import '../theme/app_spacing.dart';
import '../widgets/app_toggle_tile.dart';
import '../widgets/placeholder_view.dart';

/// Apps: the user's real installed apps, each with a bubble toggle.
/// Every app is off until the user turns it on here.
class AppsScreen extends StatefulWidget {
  const AppsScreen({super.key, this.active = true});

  final bool active;

  @override
  State<AppsScreen> createState() => _AppsScreenState();
}

class _AppsScreenState extends State<AppsScreen> with WidgetsBindingObserver {
  List<InstalledApp>? _apps;
  Set<String> _enabled = {};
  String _query = '';
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (widget.active) _load();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didUpdateWidget(AppsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active && !oldWidget.active) {
      if (_apps == null) {
        _load();
      } else {
        _refreshEnabled();
      }
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && widget.active) _refreshEnabled();
  }

  Future<void> _refreshEnabled() async {
    try {
      final enabled = await NativeBridge.getEnabledPackages();
      if (mounted) setState(() => _enabled = enabled);
    } catch (_) {
      if (mounted) setState(() => _failed = true);
    }
  }

  Future<void> _load() async {
    try {
      final apps = await NativeBridge.getInstalledApps();
      final enabled = await NativeBridge.getEnabledPackages();
      if (!mounted) return;
      setState(() {
        _apps = apps;
        _enabled = enabled;
        _failed = false;
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
      return Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
        const Text('Could not load your apps.'),
        const SizedBox(height: 12),
        TextButton.icon(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            label: const Text('Try again')),
      ]));
    }
    if (apps == null) {
      if (!widget.active) return const SizedBox.shrink();
      return const Center(
          child: CircularProgressIndicator(
        strokeWidth: 2,
        semanticsLabel: 'Loading apps',
      ));
    }
    final visible = _visible(apps);
    final enabled =
        visible.where((app) => _enabled.contains(app.packageName)).toList();
    final available =
        visible.where((app) => !_enabled.contains(app.packageName)).toList();
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
            decoration: const InputDecoration(
              hintText: 'Search apps',
              prefixIcon: Icon(Icons.search),
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
              : CustomScrollView(
                  primary: false,
                  slivers: [
                    for (final section in [
                      ('Enabled', enabled),
                      ('Other apps', available),
                    ]) ...[
                      if (section.$2.isNotEmpty || section.$1 == 'Enabled')
                        SliverToBoxAdapter(
                          child: Padding(
                            padding: const EdgeInsets.fromLTRB(
                                AppSpacing.page, 20, AppSpacing.page, 8),
                            child: Text('${section.$1} · ${section.$2.length}',
                                style: Theme.of(context).textTheme.titleSmall),
                          ),
                        ),
                      if (section.$1 == 'Enabled' && section.$2.isEmpty)
                        SliverToBoxAdapter(
                          child: Padding(
                            padding: const EdgeInsets.symmetric(
                                horizontal: AppSpacing.page, vertical: 8),
                            child: Text(
                              _query.trim().isEmpty
                                  ? 'Turn on an app below to keep it here.'
                                  : 'No enabled apps match your search.',
                              style: TextStyle(color: scheme.onSurfaceVariant),
                            ),
                          ),
                        ),
                      SliverList.builder(
                        itemCount: section.$2.length,
                        itemBuilder: (context, i) {
                          final app = section.$2[i];
                          return AppToggleTile(
                            key: ValueKey(app.packageName),
                            app: app,
                            enabled: _enabled.contains(app.packageName),
                            onChanged: (on) => _toggle(app, on),
                          );
                        },
                      ),
                    ],
                    const SliverToBoxAdapter(child: SizedBox(height: 24)),
                  ],
                ),
        ),
      ],
    );
  }
}
