import 'package:flutter/material.dart';

import '../models/installed_app.dart';
import '../theme/app_spacing.dart';

/// One row on the Apps screen: the app's own icon, name, and bubble switch.
class AppToggleTile extends StatelessWidget {
  const AppToggleTile({
    super.key,
    required this.app,
    required this.enabled,
    required this.onChanged,
  });

  final InstalledApp app;
  final bool enabled;
  final ValueChanged<bool> onChanged;

  static const double iconSize = 40;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final icon = app.icon;
    return SwitchListTile(
      contentPadding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.page,
        vertical: 4,
      ),
      secondary: SizedBox.square(
        dimension: iconSize,
        child: icon == null
            ? Icon(Icons.apps, color: scheme.onSurfaceVariant)
            : Image.memory(
                icon,
                gaplessPlayback: true,
                excludeFromSemantics: true,
              ),
      ),
      title: Text(app.name, maxLines: 1, overflow: TextOverflow.ellipsis),
      value: enabled,
      onChanged: onChanged,
    );
  }
}
