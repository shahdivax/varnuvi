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
import dev.voiceagent.app.overlay.BubbleService
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One continuous capture, bounded chunk uploads, and ordered final output. */
class SpeechActivity : Activity() {
    private lateinit var transcription: TranscriptionSettings.Connection
    private var recorder: ContinuousRecorder? = null
    private lateinit var directory: File
    private lateinit var panel: RecordingPanel
    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var action: android.widget.ImageButton
    private lateinit var partial: Button
    private val handler = Handler(Looper.getMainLooper())
    private val ledger = ChunkTranscript()
    private val files = mutableMapOf<Int, File>()
    private val uploads = Executors.newFixedThreadPool(2)
    private val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    @Volatile private var closed = false
    private var showingResult = false
    private var dark = false
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
        dark = appearance == "dark" || (appearance != "light" && (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES)
        setTheme(if (dark) R.style.RecordingThemeDark else R.style.RecordingThemeLight)
        super.onCreate(savedInstanceState)
        BubbleService.instance?.pauseBubble()
        try { transcription = TranscriptionSettings(this).connection() }
        catch (_: Exception) { finishResult(null, "Set up your speech server in Settings → Speech server before recording."); return }
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
            AlertDialog.Builder(this).setTitle(if (rewriteFailed) "Use your original words?" else "Use the text that's ready?")
                .setMessage(if (rewriteFailed) "We couldn't polish your words. Your original text is ready to use." else "Some words couldn't be transcribed. Only the text completed so far will be used.")
                .setNegativeButton("Go back", null)
                .setPositiveButton("Use text") { _, _ -> finishResult(ledger.text(), null) }.show()
        }
        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = false; isVerticalScrollBarEnabled = false
            addView(panel)
        }
        setContentView(scroll)
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        panel.place(window, intent.getIntExtra("keyboardTop", -1),
            intent.getIntExtra("micCenterX", -1), intent.getIntExtra("micCenterY", -1))
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
                val text = TranscriptionApi.transcribe(transcription, audio, writingDefaults.getValue("inputLanguage")) { connection ->
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
        status.text = "%02d:%02d".format(seconds / 60, seconds % 60)
        val failed = rewriteFailed || ledger.failed.isNotEmpty() || captureError != null
        title.text = when {
            processing -> "Polishing your words"
            rewriteFailed -> "Couldn't polish your words"
            !submitted -> "Listening"
            ledger.pending > 0 || !captureFinished -> "Turning speech into text"
            else -> "Let's try that again"
        }
        detail.text = when {
            rewriteFailed -> "Your words are safe. Try again, or use the original text."
            ledger.failed.isNotEmpty() -> "Couldn't finish the text. Check your connection and try again."
            captureError != null -> "Recording stopped. You can use the text completed so far."
            else -> ""
        }
        detail.visibility = if (failed && !processing) android.view.View.VISIBLE else android.view.View.GONE
        panel.setError(failed)
        action.isEnabled = !processing && recorder != null && (!submitted || rewriteFailed || (captureFinished && ledger.pending == 0 && ledger.failed.isNotEmpty()))
        panel.setBusy(processing || submitted && !failed,
            captureFinished && (rewriteFailed || ledger.failed.isNotEmpty()))
        partial.text = if (rewriteFailed) "Use original text" else "Use completed text"
        partial.visibility = if (!processing && captureFinished && ledger.pending == 0 && ledger.completed > 0 && (rewriteFailed || ledger.failed.isNotEmpty() || captureError != null)) android.view.View.VISIBLE else android.view.View.GONE
    }
    private fun cancelSession() {
        if (showingResult) { finish(); return }
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
        if (intent.getBooleanExtra("overlayDictation", false)) {
            if (!text.isNullOrBlank()) {
                val saved = try { DictationStore(this).save(text) } catch (_: RuntimeException) { false }
                showingResult = true
                val result = TranscriptPanel(this, dark, text, saved, { finish() }) { value ->
                    val copied = try {
                        getSystemService(android.content.ClipboardManager::class.java)
                            .setPrimaryClip(android.content.ClipData.newPlainText("Varnuvi", value))
                        true
                    } catch (_: RuntimeException) { false }
                    if (copied) {
                        val editsSaved = value == text || try { DictationStore(this).save(value) } catch (_: RuntimeException) { false }
                        if (!editsSaved) Toast.makeText(this, "Copied. Your edits couldn't be saved to History.", Toast.LENGTH_LONG).show()
                        finish()
                    } else Toast.makeText(this, "Could not copy. Your text is still here.", Toast.LENGTH_LONG).show()
                }
                setContentView(android.widget.ScrollView(this).apply { addView(result) })
                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
                window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                window.setLayout(minOf((resources.displayMetrics.widthPixels * .92).toInt(),
                    (360 * resources.displayMetrics.density).toInt()), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                window.attributes = window.attributes.apply { gravity = android.view.Gravity.CENTER; x = 0; y = 0 }
                return
            }
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
        BubbleService.instance?.resumeBubble()
        discardDialog?.dismiss(); cleanup(); super.onDestroy()
    }
}
