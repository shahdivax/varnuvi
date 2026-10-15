package dev.voiceagent.app

import android.content.Intent
import dev.voiceagent.app.bridge.NativeBridge
import dev.voiceagent.app.speech.SpeechActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var bridge: NativeBridge? = null
    private var pendingMicrophone: MethodChannel.Result? = null
    private var pendingSpeech: MethodChannel.Result? = null
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        bridge = NativeBridge(applicationContext, ::recognize, ::requestMicrophone, ::consumeDestination).also {
            it.attach(flutterEngine.dartExecutor.binaryMessenger)
        }
    }
    private fun consumeDestination(): String? = intent.getStringExtra("destination")?.also { intent.removeExtra("destination") }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (bridge != null) consumeDestination()?.let { bridge?.navigate(it) }
    }
    private fun requestMicrophone(result: MethodChannel.Result) {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) { result.success(true); return }
        if (pendingMicrophone != null) { result.error("busy", "Permission request already open.", null); return }
        val asked = getPreferences(0).getBoolean("askedMicrophone", false)
        if (asked && !shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
            result.success(false)
            return
        }
        getPreferences(0).edit().putBoolean("askedMicrophone", true).apply()
        pendingMicrophone = result
        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 84)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 84) return
        pendingMicrophone?.success(grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED)
        pendingMicrophone = null
    }
    private fun recognize(result: MethodChannel.Result) {
        if (pendingSpeech != null) { result.error("busy", "Dictation is already open.", null); return }
        pendingSpeech = result
        try { startActivityForResult(Intent(this, SpeechActivity::class.java), 82) }
        catch (e: Exception) { pendingSpeech = null; result.error("speech_failed", e.message, null) }
    }
    @Deprecated("Android activity result bridge")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 82) return
        val result = pendingSpeech ?: return
        pendingSpeech = null
        val error = data?.getStringExtra("error")
        if (error != null) result.error("speech_failed", error, null)
        else result.success(data?.getStringExtra("text"))
    }
    override fun onDestroy() {
        pendingMicrophone?.success(false)
        pendingMicrophone = null
        pendingSpeech?.error("cancelled", "Dictation closed. Try again.", null)
        pendingSpeech = null
        super.onDestroy()
    }
}
