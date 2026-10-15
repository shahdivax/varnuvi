import 'package:flutter/material.dart';
import '../theme/theme_controller.dart';
import '../widgets/accessibility_setup_card.dart';
import '../widgets/settings_section.dart';
import 'writing_defaults_screen.dart';
import 'openrouter_screen.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key, this.active = true});
  final bool active;
  @override
  Widget build(BuildContext context) =>
      ListView(primary: false, padding: const EdgeInsets.all(26), children: [
        Text('Settings',
            style: Theme.of(context)
                .textTheme
                .headlineLarge
                ?.copyWith(letterSpacing: -1, fontWeight: FontWeight.w600)),
        const SizedBox(height: 28),
        SettingsSection(
            title: 'Dictate in other apps',
            child: AccessibilitySetupCard(active: active)),
        SettingsSection(
            title: 'Writing',
            child: Column(children: [
              ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.edit_note_rounded),
                  title: const Text('Writing defaults'),
                  subtitle: const Text('Translation, summary, tone & style'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                          builder: (_) => const WritingDefaultsScreen()))),
              ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: const Icon(Icons.hub_outlined),
                  title: const Text('AI'),
                  subtitle: const Text('Optional writing assistant'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute<void>(
                          builder: (_) => const OpenRouterScreen()))),
            ])),
        SettingsSection(
            title: 'Appearance',
            child: ValueListenableBuilder<ThemeMode>(
                valueListenable: themeController,
                builder: (context, mode, _) => SegmentedButton<ThemeMode>(
                    showSelectedIcon: false,
                    segments: const [
                      ButtonSegment(
                          value: ThemeMode.light, label: Text('Light')),
                      ButtonSegment(value: ThemeMode.dark, label: Text('Dark')),
                      ButtonSegment(
                          value: ThemeMode.system, label: Text('System'))
                    ],
                    selected: {mode},
                    onSelectionChanged: (s) async {
                      try {
                        await themeController.setMode(s.first);
                      } catch (_) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                  content: Text('Could not save appearance.')));
                        }
                      }
                    }))),
        const SettingsSection(
            title: 'Audio & privacy',
            child: Text(
                'Recordings are sent to your transcription server over HTTP (unencrypted), then removed from this device. Saved text stays on this device. When Use AI is on, completed transcripts go to OpenRouter to follow your instructions and preferences. When off, transcription works without AI.')),
      ]);
}
