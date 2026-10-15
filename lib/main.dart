import 'package:flutter/material.dart';

import 'config/app_config.dart';
import 'bridge/native_bridge.dart';
import 'shell/app_shell.dart';
import 'theme/app_theme.dart';
import 'theme/theme_controller.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  NativeBridge.initialize();
  try {
    await themeController.load();
  } catch (_) {/* Use the system appearance if settings are unavailable. */}
  runApp(const VarnuviApp());
}

class VarnuviApp extends StatelessWidget {
  const VarnuviApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<ThemeMode>(
      valueListenable: themeController,
      builder: (context, mode, _) => MaterialApp(
        title: kAppName,
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        darkTheme: AppTheme.dark,
        themeMode: mode,
        home: const AppShell(),
      ),
    );
  }
}
