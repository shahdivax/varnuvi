package dev.voiceagent.app.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.voiceagent.app.accessibility.ServiceStatus
import dev.voiceagent.app.allowlist.AllowlistStore
import dev.voiceagent.app.apps.InstalledApps
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** The only Flutter <-> Kotlin entry point. Mirrors lib/bridge/native_bridge.dart. */
class NativeBridge(private val context: Context) : MethodChannel.MethodCallHandler {
    private val allowlist = AllowlistStore(context)
    private val main = Handler(Looper.getMainLooper())

    fun attach(messenger: BinaryMessenger) {
        MethodChannel(messenger, CHANNEL).setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getInstalledApps" -> loadApps(result)
            "getEnabledPackages" -> result.success(allowlist.enabledPackages().toList())
            "setPackageEnabled" -> setEnabled(call, result)
            "isAccessibilityServiceEnabled" ->
                result.success(ServiceStatus.isEnabled(context))
            "openAccessibilitySettings" -> {
                ServiceStatus.openSettings(context)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    // Querying the package manager can be slow; keep it off the main thread.
    private fun loadApps(result: MethodChannel.Result) {
        Thread {
            try {
                val apps = InstalledApps.launchable(context).map {
                    mapOf(
                        "name" to it.name,
                        "packageName" to it.packageName,
                        // ByteArray arrives in Dart as Uint8List.
                        "icon" to it.icon,
                    )
                }
                main.post { result.success(apps) }
            } catch (e: Exception) {
                main.post { result.error("apps_failed", e.message, null) }
            }
        }.start()
    }

    private fun setEnabled(call: MethodCall, result: MethodChannel.Result) {
        val pkg = call.argument<String>("packageName")
        val enabled = call.argument<Boolean>("enabled")
        if (pkg.isNullOrBlank() || enabled == null) {
            result.error("bad_args", "packageName and enabled are required", null)
            return
        }
        allowlist.setEnabled(pkg, enabled)
        result.success(null)
    }

    companion object {
        const val CHANNEL = "dev.voiceagent.app/bridge"
    }
}
