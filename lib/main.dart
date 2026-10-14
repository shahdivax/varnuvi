import 'package:flutter/material.dart';

import 'config/app_config.dart';

void main() {
  runApp(const VoiceAgentApp());
}

class VoiceAgentApp extends StatelessWidget {
  const VoiceAgentApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: kAppName,
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF5E6B7D)),
        scaffoldBackgroundColor: const Color(0xFFF7F3EA),
      ),
      home: const Scaffold(
        body: Center(child: Text(kAppName)),
      ),
    );
  }
}
