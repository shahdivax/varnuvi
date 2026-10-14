import 'dart:async';

import 'package:flutter/foundation.dart';

import '../models/mic_state.dart';

/// Fake dictation cycle: idle -> listening -> processing -> idle.
/// Stands in for real speech recognition, which is out of scope.
class FakeDictation extends ValueNotifier<MicState> {
  FakeDictation() : super(MicState.idle);

  static const listenFor = Duration(seconds: 3);
  static const processFor = Duration(milliseconds: 1200);

  Timer? _timer;

  /// Tap handler: starts listening when idle, stops early when listening.
  void toggle() {
    switch (value) {
      case MicState.idle:
        _go(MicState.listening, listenFor, _process);
      case MicState.listening:
        _process();
      case MicState.processing:
        break;
    }
  }

  void _process() =>
      _go(MicState.processing, processFor, () => value = MicState.idle);

  void _go(MicState next, Duration after, VoidCallback then) {
    _timer?.cancel();
    value = next;
    _timer = Timer(after, then);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}
