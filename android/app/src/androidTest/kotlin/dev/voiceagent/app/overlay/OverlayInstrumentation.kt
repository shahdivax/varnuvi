package dev.voiceagent.app.overlay

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import dev.voiceagent.app.insert.RecordingPreviewActivity

/** Exercises the real result UI without microphone, network, or clipboard writes. */
class OverlayInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun checkConnectionStorage() {
        val name = "transcription-test-${java.util.UUID.randomUUID()}"
        val isolated = object : android.content.ContextWrapper(targetContext) {
            override fun getSharedPreferences(unused: String, mode: Int) = targetContext.getSharedPreferences(name, mode)
        }
        val settings = dev.voiceagent.app.speech.TranscriptionSettings(isolated)
        val secret = "synthetic-".repeat(5)
        try {
            check(settings.publicState()["endpoint"] == "" && settings.publicState()["hasToken"] == false)
            check(runCatching { settings.connection() }.isFailure)
            settings.save("https://speech.example.org/api/transcribe", secret, false)
            check(settings.connection().token == secret)
            check(!settings.publicState().containsKey("token"))
            check(!isolated.getSharedPreferences("unused", 0).getString("secret", "")!!.contains(secret))
            check(runCatching { settings.save("https://other.example.org/api/transcribe", "", false) }.isFailure)
            settings.save("", "", true)
            check(runCatching { settings.connection() }.isFailure)
        } finally { isolated.getSharedPreferences("unused", 0).edit().clear().commit() }
    }
    private fun checkMultipartConnection() {
        val secret = "synthetic-".repeat(5)
        val socket = java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        socket.soTimeout = 5000
        val failure = java.util.concurrent.atomic.AtomicReference<Throwable?>()
        val audio = java.io.File(targetContext.cacheDir, "connection-check.wav").apply { writeBytes(byteArrayOf(82, 73, 70, 70)) }
        val worker = Thread {
            try {
                socket.accept().use { client ->
                    client.soTimeout = 5000
                    val input = client.getInputStream().buffered()
                    fun line(): String {
                        val bytes = java.io.ByteArrayOutputStream()
                        while (true) {
                            val value = input.read()
                            check(value >= 0)
                            if (value == 10) return bytes.toString("UTF-8").trimEnd('\r')
                            bytes.write(value)
                        }
                    }
                    check(line().startsWith("POST /api/transcribe "))
                    val headers = mutableMapOf<String, String>()
                    while (true) {
                        val header = line()
                        if (header.isEmpty()) break
                        headers[header.substringBefore(':').lowercase()] = header.substringAfter(':').trim()
                    }
                    check(headers["authorization"] == "Bearer $secret")
                    val body = ByteArray(headers.getValue("content-length").toInt())
                    java.io.DataInputStream(input).readFully(body)
                    val multipart = String(body, Charsets.UTF_8)
                    check(multipart.contains("name=\"audio\""))
                    check(multipart.contains("name=\"language\"\r\n\r\ngu"))
                    check(!multipart.contains("name=\"polish\""))
                    val response = "{\"text\":\"hello ગુજરાતી\"}".toByteArray(Charsets.UTF_8)
                    client.getOutputStream().apply {
                        write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${response.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        write(response); flush()
                    }
                }
            } catch (error: Throwable) { failure.set(error) }
        }.apply { start() }
        try {
            val connection = dev.voiceagent.app.speech.TranscriptionSettings.Connection("http://localhost:${socket.localPort}/api/transcribe", secret)
            val text = dev.voiceagent.app.speech.TranscriptionApi.transcribe(connection, audio, "gu") { }
            check(text == "hello ગુજરાતી")
            worker.join(6000)
            failure.get()?.let { throw it }
            check(!worker.isAlive)
        } finally { socket.close(); audio.delete() }
    }
    override fun onStart() {
        val report = StringBuilder()
        var code = Activity.RESULT_OK
        var backdrop: Activity? = null
        try {
            checkConnectionStorage()
            checkMultipartConnection()
            val services = targetContext.packageManager.getPackageInfo(targetContext.packageName,
                android.content.pm.PackageManager.GET_SERVICES).services.orEmpty()
            check(services.none { it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE" })
            check(services.any { it.name == BubbleService::class.java.name && !it.exported })
            backdrop = startActivitySync(Intent(targetContext, dev.voiceagent.app.insert.PreviewBackdropActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            for (dark in listOf(false, true)) {
                val activity = startActivitySync(Intent(targetContext, RecordingPreviewActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("dark", dark)) as RecordingPreviewActivity
                try {
                    waitForIdleSync()
                    val panel = activity.panel
                    val copy = (0 until panel.childCount).map { panel.getChildAt(it) }
                        .filterIsInstance<Button>().single()
                    check(activity.copied == null) { "Opening the result must not copy anything" }
                    runOnMainSync {
                        panel.editor.setText("Edited words 🙂 ગુજરાતી")
                        copy.performClick()
                    }
                    check(activity.copied == "Edited words 🙂 ગુજરાતી")
                    runOnMainSync { panel.editor.setText("  "); copy.performClick() }
                    check(activity.copied == "Edited words 🙂 ગુજરાતી") { "Blank results must not replace the clipboard" }
                    check(panel.editor.error != null)
                    runOnMainSync {
                        panel.editor.error = null
                        panel.editor.setText("A little more room for your words.\n\nSpeak naturally. Copy when you're ready.")
                        panel.requestFocus()
                    }
                    waitForIdleSync()
                    SystemClock.sleep(250)
                    val screenshot = uiAutomation.takeScreenshot()
                    if (screenshot != null) {
                        java.io.File(targetContext.getExternalFilesDir(null), "overlay-result-${if (dark) "dark" else "light"}.png")
                            .outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                        screenshot.recycle()
                    }
                } finally { runOnMainSync { activity.finish() } }
            }
            report.append("PASS: authenticated multipart HTTP round-trip; empty server defaults; encrypted token round-trip; changed-host token guard; removal; no accessibility service; explicit copy only; edited Unicode text; blank-copy guard; light/dark result UI\n")
        } catch (failure: Throwable) {
            code = Activity.RESULT_CANCELED
            report.append(failure.stackTraceToString())
        }
        backdrop?.let { runOnMainSync { it.finish() } }
        finish(code, Bundle().apply { putString("stream", report.toString()) })
    }
}
