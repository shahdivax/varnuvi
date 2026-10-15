package dev.voiceagent.app.speech

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import dev.voiceagent.app.R
import dev.voiceagent.app.accessibility.VoiceAccessibilityService
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One continuous capture, bounded chunk uploads, and ordered final output. */
class SpeechActivity : Activity() {
    private var recorder: ContinuousRecorder? = null
    private lateinit var directory: File
    private lateinit var panel: RecordingPanel
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var action: Button
    private lateinit var partial: Button
    private val handler = Handler(Looper.getMainLooper())
    private val ledger = ChunkTranscript()
    private val files = mutableMapOf<Int, File>()
    private val uploads = Executors.newFixedThreadPool(2)
    private val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    @Volatile private var closed = false
    private var handedOff = false
    private var processing = false
    private var rewriteFailed = false
    private var rewriteAttempted = false
    private var writingDefaults = dev.voiceagent.app.writing.WritingSettings.emptyDefaults()
    private var writingKey = ""
    private var writingModel = ""
    private var useWriting = false
    private var submitted = false
    private var captureFinished = false
    private var captureError: String? = null
    private var uploadError: String? = null
    private var discardDialog: AlertDialog? = null
    private var displayedSeconds = -1
    private val tick = object : Runnable {
        override fun run() {
            if (closed) return
            if (displayedSeconds != recorder?.seconds) render()
            panel.meter.push(if (!submitted) recorder?.level ?: 0f else 0f)
            if (!submitted) handler.postDelayed(this, 120)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        val appearance = getSharedPreferences("veya_settings", 0).getString("theme", "system")
        val dark = appearance == "dark" || (appearance != "light" && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
        setTheme(if (dark) R.style.RecordingThemeDark else R.style.RecordingThemeLight)
        super.onCreate(savedInstanceState)
        val settings = dev.voiceagent.app.writing.WritingSettings(this)
        writingDefaults = settings.defaults()
        useWriting = settings.aiEnabled()
        if (useWriting) {
            writingModel = settings.model()
            writingKey = try { settings.apiKey() } catch (_: Exception) { "" }
        }
        directory = File(cacheDir, "veya-session-${java.util.UUID.randomUUID()}")
        if (!directory.mkdirs()) { finishResult(null, "Could not prepare recording storage."); return }
        panel = RecordingPanel(this, dark) { cancelSession() }
        title = panel.title; status = panel.timer; detail = panel.detail
        action = panel.action; partial = panel.partial
        action.setOnClickListener { if (rewriteFailed) { rewriteFailed = false; rewriteAttempted = false; settle() } else if (captureFinished) retryFailed() else submit() }
        partial.setOnClickListener {
            AlertDialog.Builder(this).setTitle(if (rewriteFailed) "Use the original transcript?" else "Use an incomplete transcript?")
                .setMessage(if (rewriteFailed) "AI processing could not be completed. Your original transcript is complete." else "Only ${ledger.completed} of ${ledger.size} chunks are ready. Missing audio will be discarded.")
                .setNegativeButton("Keep waiting", null)
                .setPositiveButton("Use completed text") { _, _ -> finishResult(ledger.text(), null) }.show()
        }
        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = false; isVerticalScrollBarEnabled = false
            addView(panel)
        }
        setContentView(scroll)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(.62f)
        window.setLayout(minOf((resources.displayMetrics.widthPixels * .92).toInt(), panel.dp(420)), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        setFinishOnTouchOutside(false)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startRecording()
        else requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 83)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != 83) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startRecording()
        else finishResult(null, "Microphone access is off. Enable it in Varnuvi's bubble setup or Android app permissions.")
    }
    private fun startRecording() {
        try {
            recorder = ContinuousRecorder(directory,
                onChunk = { file -> handler.post {
                    if (closed) file.delete()
                    else { val index = ledger.add(); files[index] = file; enqueue(index); render() }
                } },
                onFinished = { error -> handler.post {
                    if (!closed) {
                        captureFinished = true; submitted = true; captureError = error ?: captureError
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        settle()
                    }
                } },
            ).also { it.start() }
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            handler.post(tick)
        } catch (_: Exception) { finishResult(null, "Could not start the microphone. Check permissions and try again.") }
    }
    private fun submit() {
        if (submitted || closed) return // Debounce Submit while capture drains its final chunk.
        submitted = true
        recorder?.stop()
        render()
    }
    private fun enqueue(index: Int) {
        val audio = files[index] ?: return
        uploads.execute {
            var ownConnection: HttpURLConnection? = null
            try {
                if (closed) return@execute
                val text = TranscriptionApi.transcribe(audio, writingDefaults.getValue("inputLanguage")) { connection ->
                    ownConnection = connection; connections.add(connection)
                    if (closed) { connection.disconnect(); throw java.io.IOException("Cancelled") }
                }
                handler.post {
                    if (!closed) {
                        ledger.complete(index, text)
                        files.remove(index)?.delete()
                        settle()
                    }
                }
            } catch (e: Exception) {
                handler.post {
                    if (!closed) {
                        ledger.fail(index)
                        uploadError = when (e) {
                            is java.net.SocketTimeoutException -> "Server timed out."
                            is java.net.ConnectException, is java.net.UnknownHostException -> "Could not reach the server."
                            else -> e.message ?: "A chunk could not be transcribed."
                        }
                        settle()
                    }
                }
            } finally { ownConnection?.let { connections.remove(it) } }
        }
    }
    private fun retryFailed() {
        if (closed || !captureFinished || ledger.pending != 0) return
        uploadError = null
        // Mark pending before scheduling: rapid taps cannot enqueue the same chunk twice.
        for (index in ledger.failed) { if (ledger.retry(index)) enqueue(index) }
        render()
    }
    private fun settle() {
        if (closed) return
        if (submitted && captureFinished && ledger.pending == 0 && ledger.failed.isEmpty() && captureError == null) {
            val text = ledger.text()
            if (text.isNotBlank()) {
                if (useWriting) processWriting(text)
                else finishResult(text, null)
            }
            else finishResult(null, "Recording was empty. Please try again.")
        } else render()
    }
    private fun processWriting(text: String) {
        if (rewriteAttempted || closed) { render(); return }
        rewriteAttempted = true; processing = true; rewriteFailed = false
        uploads.execute {
            var own: HttpURLConnection? = null
            try {
                val output = dev.voiceagent.app.writing.OpenRouter.transform(writingKey, writingModel, writingDefaults, text) { next ->
                    own = next; connections.add(next)
                    if (closed) { next.disconnect(); throw java.io.IOException("Cancelled") }
                }
                handler.post { if (!closed) { processing = false; finishResult(output, null) } }
            } catch (e: Exception) {
                handler.post { if (!closed) {
                    processing = false; rewriteFailed = true
                    uploadError = e.message?.takeUnless { it.contains("sk-or-") } ?: "AI processing could not be completed."
                    render()
                } }
            } finally { own?.let { connections.remove(it) } }
        }
        render()
    }
    private fun render() {
        if (!::title.isInitialized || closed) return
        val seconds = recorder?.seconds ?: 0
        displayedSeconds = seconds
        title.text = if (processing) "Shaping your words" else if (rewriteFailed) "AI needs attention" else if (!submitted) "Listening" else if (ledger.pending > 0 || !captureFinished) "Finishing up" else "Needs attention"
        status.text = "%02d:%02d".format(seconds / 60, seconds % 60)
        val progress = "${ledger.completed} of ${ledger.size} ${if (ledger.size == 1) "part" else "parts"} transcribed"
        detail.text = if (processing) "Following your instructions and preferences" else if (rewriteFailed) uploadError.orEmpty() else if (ledger.failed.isNotEmpty()) "$progress · ${ledger.failed.size} need retry\n${uploadError.orEmpty()}"
            else if (captureError != null) "$progress\n$captureError"
            else if (ledger.size == 0) "Transcription starts at 0:30"
            else progress
        panel.setProgress(ledger.completed, ledger.size, ledger.failed.isNotEmpty() || captureError != null)
        action.text = if (rewriteFailed) "Retry AI processing" else if (!submitted) "Submit" else if (captureFinished && ledger.failed.isNotEmpty()) "Retry failed chunks" else "Finishing…"
        action.isEnabled = !processing && recorder != null && (!submitted || rewriteFailed || (captureFinished && ledger.pending == 0 && ledger.failed.isNotEmpty()))
        partial.text = if (rewriteFailed) "Use original transcript" else "Use completed text"
        partial.visibility = if (!processing && captureFinished && ledger.pending == 0 && ledger.completed > 0 && (rewriteFailed || ledger.failed.isNotEmpty() || captureError != null)) android.view.View.VISIBLE else android.view.View.GONE
    }
    private fun cancelSession() {
        if (closed || discardDialog?.isShowing == true) return
        if ((recorder?.seconds ?: 0) < 3 && ledger.size == 0) { finishResult(null, null); return }
        discardDialog = AlertDialog.Builder(this).setTitle("Discard this session?")
            .setMessage("Your recording and processed text will be removed.")
            .setNegativeButton("Keep session", null)
            .setPositiveButton("Discard") { _, _ -> finishResult(null, null) }.show()
    }
    private fun cleanup() {
        if (closed) return
        closed = true
        handler.removeCallbacksAndMessages(null)
        recorder?.stop(cancel = true)
        connections.forEach { it.disconnect() }
        uploads.shutdownNow()
        if (::directory.isInitialized) Thread {
            recorder?.awaitStop()
            uploads.awaitTermination(100, TimeUnit.SECONDS)
            directory.deleteRecursively()
        }.start()
    }
    private fun finishResult(text: String?, error: String?) {
        if (closed) return
        cleanup()
        val target = intent.getStringExtra("targetPackage")
        if (target != null) {
            if (text != null) {
                val saved = DictationStore(this).save(text)
                handedOff = VoiceAccessibilityService.instance?.deliver(text, target, saved, intent.getStringExtra("targetSession")) == true
                if (!handedOff) Toast.makeText(this, if (saved) "Your transcript is in Varnuvi History. The original field is no longer available." else "Could not insert or save. Please try again.", Toast.LENGTH_LONG).show()
            }
            if (text == null) VoiceAccessibilityService.instance?.cancelDictation(intent.getStringExtra("targetSession"))
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        } else setResult(if (error == null) RESULT_OK else RESULT_FIRST_USER, Intent().putExtra("text", text).putExtra("error", error))
        finish()
    }
    @Deprecated("Android back bridge")
    override fun onBackPressed() { cancelSession() }
    override fun onPause() {
        super.onPause()
        // Keep the existing visible-recording contract; no hidden/background microphone capture.
        if (recorder != null && !submitted && !closed) {
            captureError = "Recording stopped because Varnuvi left the screen."
            submit()
        }
    }
    override fun onDestroy() {
        if (!handedOff) VoiceAccessibilityService.instance?.cancelDictation(intent.getStringExtra("targetSession"))
        discardDialog?.dismiss(); cleanup(); super.onDestroy()
    }
}
