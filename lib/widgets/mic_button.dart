import 'package:flutter/material.dart';

import '../models/mic_state.dart';

/// Large circular mic button, the single primary action on Home.
/// Listening shows a soft pulse ring; processing shows a thin spinner.
class MicButton extends StatefulWidget {
  const MicButton({super.key, required this.state, required this.onPressed});

  final MicState state;
  final VoidCallback onPressed;

  static const double size = 120;

  @override
  State<MicButton> createState() => _MicButtonState();
}

class _MicButtonState extends State<MicButton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 1400),
  );

  @override
  void initState() {
    super.initState();
    _sync();
  }

  @override
  void didUpdateWidget(MicButton oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.state != widget.state) _sync();
  }

  void _sync() {
    if (widget.state == MicState.listening) {
      _pulse.repeat();
    } else {
      _pulse.reset();
    }
  }

  @override
  void dispose() {
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final state = widget.state;
    final busy = state == MicState.processing;
    return Semantics(
      button: true,
      enabled: !busy,
      label: state.semanticLabel,
      child: SizedBox.square(
        dimension: MicButton.size * 1.4,
        child: Stack(
          alignment: Alignment.center,
          children: [
            if (state == MicState.listening) _PulseRing(animation: _pulse),
            if (busy)
              SizedBox.square(
                dimension: MicButton.size + 16,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: scheme.primary,
                ),
              ),
            Material(
              color: busy ? scheme.surfaceContainerHigh : scheme.primary,
              shape: const CircleBorder(),
              child: InkWell(
                customBorder: const CircleBorder(),
                onTap: busy ? null : widget.onPressed,
                child: SizedBox.square(
                  dimension: MicButton.size,
                  child: Icon(
                    _icon(state),
                    size: 48,
                    color: busy ? scheme.onSurfaceVariant : scheme.onPrimary,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  static IconData _icon(MicState state) => switch (state) {
        MicState.idle => Icons.mic,
        MicState.listening => Icons.stop_rounded,
        MicState.processing => Icons.more_horiz,
      };
}

/// Expanding, fading ring behind the button while listening.
class _PulseRing extends StatelessWidget {
  const _PulseRing({required this.animation});

  final Animation<double> animation;

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.primaryContainer;
    return AnimatedBuilder(
      animation: animation,
      builder: (context, _) {
        final t = Curves.easeOut.transform(animation.value);
        final d = MicButton.size * (1 + 0.35 * t);
        return Opacity(
          opacity: 1 - t,
          child: Container(
            width: d,
            height: d,
            decoration: BoxDecoration(shape: BoxShape.circle, color: color),
          ),
        );
      },
    );
  }
}
