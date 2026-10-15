import 'package:flutter/material.dart';

import '../mock/mock_languages.dart';
import '../models/transcript_mode.dart';
import '../settings/app_settings.dart';
import '../theme/app_spacing.dart';
import '../theme/theme_controller.dart';
import '../widgets/accessibility_setup_card.dart';
import '../widgets/account_placeholder_card.dart';
import '../widgets/mode_chips.dart';
import '../widgets/settings_section.dart';

/// Settings: bubble service setup, theme, default mode, translation
/// language, account placeholder.
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: AppSpacing.pageInsets,
      children: [
        const SettingsSection(
          title: 'Bubble',
          child: AccessibilitySetupCard(),
        ),
        SettingsSection(
          title: 'Theme',
          child: ValueListenableBuilder<ThemeMode>(
            valueListenable: themeController,
            builder: (context, mode, _) => SegmentedButton<ThemeMode>(
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: ThemeMode.light, label: Text('Light')),
                ButtonSegment(value: ThemeMode.dark, label: Text('Dark')),
                ButtonSegment(value: ThemeMode.system, label: Text('System')),
              ],
              selected: {mode},
              onSelectionChanged: (s) => themeController.setMode(s.first),
            ),
          ),
        ),
        SettingsSection(
          title: 'Default mode',
          child: ValueListenableBuilder<TranscriptMode>(
            valueListenable: appSettings.defaultMode,
            builder: (context, mode, _) => ModeChips(
              selected: mode,
              onChanged: (m) => appSettings.defaultMode.value = m,
            ),
          ),
        ),
        SettingsSection(
          title: 'Translate to',
          child: ValueListenableBuilder<String>(
            valueListenable: appSettings.translationLanguage,
            builder: (context, lang, _) => DropdownButton<String>(
              value: lang,
              isExpanded: true,
              underline: const SizedBox.shrink(),
              items: [
                for (final l in kMockLanguages)
                  DropdownMenuItem(value: l, child: Text(l)),
              ],
              onChanged: (l) {
                if (l != null) appSettings.translationLanguage.value = l;
              },
            ),
          ),
        ),
        const SettingsSection(
          title: 'Account',
          child: AccountPlaceholderCard(),
        ),
      ],
    );
  }
}
