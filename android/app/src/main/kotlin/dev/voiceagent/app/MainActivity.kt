package dev.voiceagent.app

import dev.voiceagent.app.bridge.NativeBridge
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        NativeBridge(applicationContext).attach(flutterEngine.dartExecutor.binaryMessenger)
    }
}
