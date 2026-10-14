/// A launchable app reported by the native layer.
class InstalledApp {
  const InstalledApp({required this.name, required this.packageName});

  factory InstalledApp.fromMap(Map<Object?, Object?> map) => InstalledApp(
        name: map['name'] as String? ?? '',
        packageName: map['packageName'] as String? ?? '',
      );

  final String name;
  final String packageName;
}
