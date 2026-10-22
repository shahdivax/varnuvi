import 'package:flutter/widgets.dart';

/// Shared layout spacing. Every screen uses the same side padding.
abstract final class AppSpacing {
  static const double page = 24;
  static const EdgeInsets pageInsets = EdgeInsets.fromLTRB(page, 16, page, 24);
}
