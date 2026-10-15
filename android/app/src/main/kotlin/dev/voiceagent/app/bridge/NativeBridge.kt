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
class NativeBridge(private val context: Context, private val recognize: (MethodChannel.Result) -> Unit, private val requestMicrophone: (MethodChannel.Result) -> Unit, private val consumeDestination: () -> String?) : MethodChannel.MethodCallHandler {
    private var channel: MethodChannel? = null
    private val writing = dev.voiceagent.app.writing.WritingSettings(context)
    private val allowlist = AllowlistStore(context)
    private val main = Handler(Looper.getMainLooper())

    fun attach(messenger: BinaryMessenger) {
        channel = MethodChannel(messenger, CHANNEL).also { it.setMethodCallHandler(this) }
    }

    fun navigate(destination: String) { channel?.invokeMethod("navigate", destination) }
    private fun background(result: MethodChannel.Result, work: () -> Any?) {
        Thread { try { val value = work(); main.post { result.success(value) } }
            catch (e: Exception) { main.post { result.error("writing_failed", when (e) {
                is java.net.SocketTimeoutException -> "OpenRouter timed out. Please try again."
                is java.net.UnknownHostException, is java.net.ConnectException -> "Could not reach OpenRouter. Check your connection."
                is java.security.GeneralSecurityException -> "Your saved key is unavailable. Enter it again."
                else -> e.message?.takeUnless { it.contains("sk-or-") } ?: "Could not update writing settings."
            }, null) } }
        }.start()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "consumeDestination" -> result.success(consumeDestination())
            "getWritingSettings" -> result.success(writing.publicState())
            "saveWritingDefaults" -> background(result) {
                writing.saveDefaults(call.argument<Map<String, String>>("defaults") ?: emptyMap()); writing.publicState()
            }
            "saveOpenRouter" -> background(result) {
                writing.saveConnection(call.argument<String>("model").orEmpty(), call.argument<String>("key"), call.argument<Boolean>("clearKey") == true); writing.publicState()
            }
            "getOpenRouterModels" -> background(result) { dev.voiceagent.app.writing.OpenRouter.models() }
            "testOpenRouter" -> background(result) {
                val revision = writing.connectionRevision()
                val response = dev.voiceagent.app.writing.OpenRouter.test(writing.apiKey(), writing.model())
                writing.markTested(revision)
                response
            }
            "setAiEnabled" -> background(result) {
                val enabled = call.argument<Boolean>("enabled") ?: throw IllegalArgumentException("Choose whether to use AI.")
                writing.setAiEnabled(enabled); writing.publicState()
            }
            "requestMicrophone" -> requestMicrophone(result)
            "getBubbleStatus" -> result.success(mapOf(
                "microphone" to (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED),
                "enabled" to ServiceStatus.isEnabled(context),
                "connected" to (dev.voiceagent.app.accessibility.VoiceAccessibilityService.instance != null),
                "dictating" to (dev.voiceagent.app.accessibility.VoiceAccessibilityService.instance?.isDictating == true),
                "visible" to (dev.voiceagent.app.accessibility.VoiceAccessibilityService.instance?.isBubbleShowing == true),
                "appCount" to allowlist.enabledPackages().size,
            ))
            "recognizeSpeech" -> recognize(result)
            "getHistory" -> result.success(dev.voiceagent.app.speech.DictationStore(context).read().toString())
            "saveTranscript" -> {
                val text = call.argument<String>("text")
                if (text.isNullOrBlank()) result.error("empty", "There is no text to save.", null)
                else if (dev.voiceagent.app.speech.DictationStore(context).save(text)) result.success(null)
                else result.error("save_failed", "Could not save this transcript.", null)
            }
            "clearHistory" -> {
                if (dev.voiceagent.app.speech.DictationStore(context).clear()) result.success(null)
                else result.error("save_failed", "Could not clear history.", null)
            }
            "getTheme" -> result.success(context.getSharedPreferences("veya_settings", 0).getString("theme", "system"))
            "setTheme" -> {
                context.getSharedPreferences("veya_settings", 0).edit().putString("theme", call.argument<String>("theme")).apply()
                result.success(null)
            }
            "getInstalledApps" -> loadApps(result)
            "getEnabledPackages" -> result.success(allowlist.enabledPackages().toList())
            "setPackageEnabled" -> setEnabled(call, result)
            "isAccessibilityServiceEnabled" ->
                result.success(ServiceStatus.isEnabled(context))
            "openAccessibilitySettings" -> {
                try {
                    ServiceStatus.openSettings(context)
                    result.success(null)
                } catch (_: android.content.ActivityNotFoundException) {
                    result.error("settings_unavailable", "Android accessibility settings are unavailable on this device.", null)
                } catch (_: SecurityException) {
                    result.error("settings_blocked", "Android blocked opening accessibility settings. Open Settings > Accessibility and select Varnuvi.", null)
                }
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
