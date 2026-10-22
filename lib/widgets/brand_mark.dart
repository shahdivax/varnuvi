import 'package:flutter/material.dart';

class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 36});
  final double size;
  @override
  Widget build(BuildContext context) => Semantics(
      label: 'Varnuvi',
      image: true,
      child: SizedBox.square(
          dimension: size,
          child: CustomPaint(
              painter: _Mark(Theme.of(context).colorScheme.primary))));
}

class _Mark extends CustomPainter {
  const _Mark(this.color);
  final Color color;
  @override
  void paint(Canvas canvas, Size size) {
    canvas.scale(size.width / 76, size.height / 76);
    canvas.translate(-16, -20);
    final p = Path()
      ..moveTo(26, 32)
      ..cubicTo(36, 32, 40, 40, 44, 51)
      ..lineTo(51, 69)
      ..cubicTo(52, 72, 56, 72, 57, 69)
      ..lineTo(73, 32)
      ..lineTo(84, 32)
      ..lineTo(64, 78)
      ..cubicTo(60, 87, 48, 87, 44, 78)
      ..lineTo(33, 50)
      ..cubicTo(30, 42, 27, 41, 23, 41)
      ..close();
    canvas.drawPath(p, Paint()..color = color);
    canvas.drawRRect(
        RRect.fromRectAndRadius(
            const Rect.fromLTWH(53, 32, 11, 18), const Radius.circular(3)),
        Paint()..color = const Color(0xFFBFCBFF));
  }

  @override
  bool shouldRepaint(covariant _Mark oldDelegate) => color != oldDelegate.color;
}
