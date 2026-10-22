package dev.voiceagent.app.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/** The only Flutter <-> Kotlin entry point. Mirrors lib/bridge/native_bridge.dart. */
class NativeBridge(private val context: Context, private val recognize: (MethodChannel.Result) -> Unit, private val requestMicrophone: (MethodChannel.Result) -> Unit, private val consumeDestination: () -> String?) : MethodChannel.MethodCallHandler {
    private var channel: MethodChannel? = null
    private val transcription = dev.voiceagent.app.speech.TranscriptionSettings(context)
    private val writing = dev.voiceagent.app.writing.WritingSettings(context)
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

    private fun serverWork(result: MethodChannel.Result, work: () -> Any?) {
        Thread {
            try { val value = work(); main.post { result.success(value) } }
            catch (error: Exception) {
                val message = if (error is IllegalArgumentException || error is IllegalStateException) error.message
                    else "Could not connect to your speech server. Check the URL, token, and network."
                main.post { result.error("server_failed", message, null) }
            }
        }.start()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "consumeDestination" -> result.success(consumeDestination())
            "getTranscriptionSettings" -> result.success(transcription.publicState())
            "saveTranscriptionSettings" -> serverWork(result) {
                transcription.save(call.argument<String>("endpoint").orEmpty(), call.argument<String>("token"), call.argument<Boolean>("clear") == true)
                transcription.publicState()
            }
            "testTranscriptionServer" -> serverWork(result) { transcription.test() }
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
                "enabled" to android.provider.Settings.canDrawOverlays(context),
                "connected" to (dev.voiceagent.app.overlay.BubbleService.instance != null),
                "dictating" to (dev.voiceagent.app.overlay.BubbleService.instance?.dictating == true),
                "visible" to (dev.voiceagent.app.overlay.BubbleService.instance?.visible == true),
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
            "openOverlaySettings" -> {
                try {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    result.success(null)
                } catch (_: RuntimeException) {
                    result.error("settings_unavailable", "Open Settings > Apps > Special app access > Display over other apps > Varnuvi.", null)
                }
            }
            "setBubbleEnabled" -> {
                val enabled = call.argument<Boolean>("enabled")
                if (enabled == null) result.error("bad_args", "Choose whether to show the bubble.", null)
                else try {
                    val intent = android.content.Intent(context, dev.voiceagent.app.overlay.BubbleService::class.java)
                    if (enabled) {
                        check(android.provider.Settings.canDrawOverlays(context)) { "Allow display over other apps first." }
                        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            (context as? android.app.Activity)?.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 85)
                        }
                        context.startForegroundService(intent)
                    } else context.stopService(intent)
                    result.success(null)
                } catch (_: RuntimeException) {
                    result.error("bubble_failed", "Could not show the bubble. Check its display permission and try again.", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    companion object {
        const val CHANNEL = "dev.voiceagent.app/bridge"
    }
}
