import 'package:flutter/material.dart';

import 'config/app_config.dart';
import 'shell/app_shell.dart';
import 'theme/app_theme.dart';
import 'theme/theme_controller.dart';

void main() {
  runApp(const VoiceAgentApp());
}

class VoiceAgentApp extends StatelessWidget {
  const VoiceAgentApp({super.key});

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
