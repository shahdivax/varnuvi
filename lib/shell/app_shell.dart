import 'package:flutter/material.dart';
import '../screens/apps_screen.dart';
import '../screens/history_screen.dart';
import '../screens/home_screen.dart';
import '../screens/settings_screen.dart';
import '../store/transcript_store.dart';
import '../widgets/brand_mark.dart';
import '../bridge/native_bridge.dart';

class AppShell extends StatefulWidget {
  const AppShell({super.key});
  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> with WidgetsBindingObserver {
  int _index = 0;
  String? _loadError;
  List<Widget> get _pages => [
        const HomeScreen(),
        AppsScreen(active: _index == 1),
        const HistoryScreen(),
        SettingsScreen(active: _index == 3),
      ];
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _reload();
    NativeBridge.destination.addListener(_navigate);
    _initialDestination();
  }

  Future<void> _initialDestination() async {
    try {
      final target = await NativeBridge.consumeDestination();
      if (mounted && target != null) {
        setState(() => _index = target == 'settings' ? 3 : 0);
      }
    } catch (_) {/* Normal launches do not need a navigation target. */}
  }

  void _navigate() {
    final target = NativeBridge.destination.value;
    if (target == null || !mounted) return;
    NativeBridge.destination.value = null;
    setState(() => _index = target == 'settings' ? 3 : 0);
  }

  Future<void> _reload() async {
    try {
      await transcripts.reload();
      if (mounted) setState(() => _loadError = null);
    } catch (_) {
      if (mounted) {
        setState(() =>
            _loadError = 'Could not load saved dictations. Tap to retry.');
      }
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _reload();
  }

  @override
  void dispose() {
    NativeBridge.destination.removeListener(_navigate);
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
            toolbarHeight: 76,
            titleSpacing: 26,
            title: const Row(children: [
              BrandMark(size: 34),
              SizedBox(width: 11),
              Text('varnuvi',
                  style: TextStyle(
                      fontSize: 27,
                      fontWeight: FontWeight.w600,
                      letterSpacing: -1.1))
            ])),
        body: SafeArea(
            child: Column(children: [
          if (_loadError != null)
            MaterialBanner(content: Text(_loadError!), actions: [
              TextButton(onPressed: _reload, child: const Text('Retry'))
            ]),
          Expanded(child: IndexedStack(index: _index, children: _pages))
        ])),
        bottomNavigationBar: NavigationBar(
            height: 76,
            backgroundColor: Theme.of(context).colorScheme.surface,
            selectedIndex: _index,
            onDestinationSelected: (i) {
              setState(() => _index = i);
              if (i == 2) _reload();
            },
            destinations: const [
              NavigationDestination(
                  icon: Icon(Icons.graphic_eq_rounded), label: 'Speak'),
              NavigationDestination(
                  icon: Icon(Icons.widgets_outlined), label: 'Apps'),
              NavigationDestination(
                  icon: Icon(Icons.bookmarks_outlined), label: 'History'),
              NavigationDestination(
                  icon: Icon(Icons.tune_rounded), label: 'Settings')
            ]),
      );
}
