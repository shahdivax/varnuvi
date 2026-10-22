package dev.voiceagent.app.writing

import android.content.Context
import org.json.JSONObject
import dev.voiceagent.app.security.EncryptedSecret

class WritingSettings(context: Context) {
    private val prefs = context.getSharedPreferences("varnuvi_writing", Context.MODE_PRIVATE)
    companion object {
        val options = WritingPolicy.options
        fun emptyDefaults() = WritingPolicy.emptyDefaults()
    }
    fun defaults(): Map<String, String> {
        val json = try { JSONObject(prefs.getString("defaults", "{}")!!) } catch (_: Exception) { JSONObject() }
        return emptyDefaults().mapValues { (key, fallback) -> json.optString(key).takeIf { it in options.getValue(key) } ?: fallback }
    }
    fun saveDefaults(values: Map<String, String>) {
        require(values.keys == options.keys && values.all { (key, value) -> value in options.getValue(key) }) { "Invalid writing preferences." }
        check(prefs.edit().putString("defaults", JSONObject(values).toString()).commit()) { "Could not save defaults." }
    }
    fun connectionRevision() = prefs.getLong("connection_revision", 0)
    fun isTested() = prefs.getBoolean("tested", false) && isConfigured()
    fun aiEnabled() = WritingPolicy.shouldProcess(prefs.getBoolean("ai_enabled", false), isTested(), isConfigured())
    @Synchronized fun markTested(revision: Long) {
        check(revision == connectionRevision() && isConfigured()) { "AI settings changed during the test. Test again." }
        check(prefs.edit().putBoolean("tested", true).commit()) { "Could not save the test result." }
    }
    @Synchronized fun setAiEnabled(enabled: Boolean) {
        check(!enabled || isTested()) { "Test your AI connection successfully before turning it on." }
        check(prefs.edit().putBoolean("ai_enabled", enabled).commit()) { "Could not save AI preference." }
    }
    fun isConfigured() = prefs.contains("secret") && model().isNotBlank()
    fun model(): String = prefs.getString("model", "").orEmpty()
    fun apiKey(): String = prefs.getString("secret", null)?.let {
        EncryptedSecret.decrypt("varnuvi_openrouter", it)
    } ?: ""
    fun publicState(): Map<String, Any> = mapOf("model" to model(), "hasKey" to prefs.contains("secret"), "defaults" to defaults(), "tested" to isTested(), "aiEnabled" to aiEnabled())
    @Synchronized fun saveConnection(model: String, key: String?, clear: Boolean) {
        require(model.length <= 200 && model.none { it == '\r' || it == '\n' }) { "Invalid model ID." }
        val changed = clear || !key.isNullOrBlank() || model.trim() != model()
        val edit = prefs.edit().putString("model", model.trim())
        if (changed) edit.putBoolean("tested", false).putBoolean("ai_enabled", false)
            .putLong("connection_revision", connectionRevision() + 1)
        if (clear) edit.remove("secret")
        else if (!key.isNullOrBlank()) {
            require(key.length in 10..512 && key.none { it.isWhitespace() }) { "Enter a valid API key." }
            edit.putString("secret", EncryptedSecret.encrypt("varnuvi_openrouter", key))
        }
        check(edit.commit()) { "Could not save OpenRouter settings." }
    }
}
