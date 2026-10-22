package dev.voiceagent.app.speech

import java.net.URI

/** Public servers require HTTPS; plain HTTP is restricted to local development. */
object TranscriptionEndpoint {
    fun validate(value: String): String {
        val uri = try { URI(value.trim()) } catch (_: Exception) { throw IllegalArgumentException("Enter a valid server URL.") }
        require(uri.host != null && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            (uri.port == -1 || uri.port in 1..65535)) { "Enter a server URL without credentials, a query, or a fragment." }
        require(uri.path == "/api/transcribe") { "The URL must end with /api/transcribe." }
        require(uri.host.lowercase() !in setOf("x.xx.xxx", "your-server.example")) { "Replace the example with your own server address." }
        require(uri.scheme == "https" || (uri.scheme == "http" && uri.host.lowercase() in setOf("localhost", "127.0.0.1", "10.0.2.2"))) {
            "Use HTTPS. HTTP is available only for localhost, USB forwarding, or the Android emulator."
        }
        return uri.toASCIIString()
    }
}
