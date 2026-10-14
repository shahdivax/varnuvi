import 'dart:typed_data';

/// A launchable app reported by the native layer, with its own icon as PNG.
class InstalledApp {
  const InstalledApp({
    required this.name,
    required this.packageName,
    this.icon,
  });

  factory InstalledApp.fromMap(Map<Object?, Object?> map) => InstalledApp(
        name: map['name'] as String? ?? '',
        packageName: map['packageName'] as String? ?? '',
        icon: map['icon'] as Uint8List?,
      );

  final String name;
  final String packageName;

  /// PNG bytes of the app's launcher icon; null if unavailable.
  final Uint8List? icon;
}
