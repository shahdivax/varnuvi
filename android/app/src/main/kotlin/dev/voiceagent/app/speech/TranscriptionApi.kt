package dev.voiceagent.app.speech

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID

/** One multipart request for both in-app and bubble dictation. */
object TranscriptionApi {
    const val ENDPOINT = "http://32.198.66.247:8000/api/transcribe"

    fun transcribe(audio: File, language: String = "auto", onConnection: (HttpURLConnection) -> Unit): String {
        val boundary = "Varnuvi-${UUID.randomUUID()}"
        val connection = URI(ENDPOINT).toURL().openConnection() as HttpURLConnection
        onConnection(connection)
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 90000
            connection.instanceFollowRedirects = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setRequestProperty("Accept", "application/json")
            // Fixed length avoids chunked-upload incompatibilities with some proxies.
            val prefix = "--$boundary\r\nContent-Disposition: form-data; name=\"language\"\r\n\r\n$language\r\n" +
                "--$boundary\r\nContent-Disposition: form-data; name=\"polish\"\r\n\r\ntrue\r\n" +
                "--$boundary\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"${audio.name}\"\r\nContent-Type: audio/wav\r\n\r\n"
            val head = prefix.toByteArray(Charsets.UTF_8)
            val tail = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(head.size.toLong() + audio.length() + tail.size)
            connection.outputStream.use { output ->
                output.write(head)
                audio.inputStream().use { it.copyTo(output) }
                output.write(tail)
            }
            val status = connection.responseCode
            if (status !in 200..299) throw java.io.IOException("Transcription server returned HTTP $status. Please try again.")
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(response)
            val text = (json.opt("text") as? String)?.trim()
            if (text.isNullOrEmpty()) throw java.io.IOException("No speech was returned. Please record again.")
            return text
        } finally { connection.disconnect() }
    }
}
