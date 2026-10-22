package dev.voiceagent.app.speech

import android.content.Context
import dev.voiceagent.app.security.EncryptedSecret
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

class TranscriptionSettings(context: Context) {
    private val prefs = context.getSharedPreferences("varnuvi_transcription", Context.MODE_PRIVATE)
    data class Connection(val endpoint: String, val token: String)
    fun publicState(): Map<String, Any> = mapOf("endpoint" to prefs.getString("endpoint", "").orEmpty(), "hasToken" to prefs.contains("secret"))
    @Synchronized fun save(endpoint: String, token: String?, clear: Boolean) {
        if (clear) {
            check(prefs.edit().clear().commit()) { "Could not remove the server connection." }
            return
        }
        val url = TranscriptionEndpoint.validate(endpoint)
        require(!token.isNullOrBlank() || (url == prefs.getString("endpoint", "") && prefs.contains("secret"))) { "Enter the access token for this server." }
        val edit = prefs.edit().putString("endpoint", url)
        if (!token.isNullOrBlank()) {
            require(token.length in 32..512 && token.all { it.code in 33..126 }) { "Use an access token of 32–512 printable characters, without spaces." }
            edit.putString("secret", EncryptedSecret.encrypt("varnuvi_transcription", token))
        }
        check(edit.commit()) { "Could not save the server connection." }
    }
    @Synchronized fun connection(): Connection {
        val endpoint = prefs.getString("endpoint", "").orEmpty()
        check(endpoint.isNotBlank() && prefs.contains("secret")) { "Set up your speech server in Settings → Speech server before recording." }
        return Connection(TranscriptionEndpoint.validate(endpoint), EncryptedSecret.decrypt("varnuvi_transcription", prefs.getString("secret", "")!!))
    }
    fun test(): String {
        val saved = connection()
        val health = URI(saved.endpoint).resolve("/health").toURL().openConnection() as HttpURLConnection
        try {
            health.connectTimeout = 15000; health.readTimeout = 15000; health.instanceFollowRedirects = false
            health.setRequestProperty("Authorization", "Bearer ${saved.token}")
            check(health.responseCode == 200) { "Could not connect. Check your server URL and access token." }
            val json = JSONObject(health.inputStream.bufferedReader().use { it.readText() })
            check(json.optString("status") == "ok") { "This address did not return a Varnuvi server health response." }
            return if (json.optBoolean("model_loaded")) "Server connected." else "Server connected. First dictation may take longer while the model loads."
        } finally { health.disconnect() }
    }
}
