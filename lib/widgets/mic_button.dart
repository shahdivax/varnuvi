import 'package:flutter/material.dart';

/// Large circular mic button, the single primary action on Home.
class MicButton extends StatelessWidget {
  const MicButton({super.key, required this.onPressed});

  final VoidCallback onPressed;

  static const double size = 120;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      button: true,
      label: 'Start dictation',
      child: Material(
        color: scheme.primary,
        shape: const CircleBorder(),
        child: InkWell(
          customBorder: const CircleBorder(),
          onTap: onPressed,
          child: SizedBox(
            width: size,
            height: size,
            child: Icon(Icons.mic, size: 48, color: scheme.onPrimary),
          ),
        ),
      ),
    );
  }
}
