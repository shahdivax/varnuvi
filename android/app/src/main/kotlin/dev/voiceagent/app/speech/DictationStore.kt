package dev.voiceagent.app.speech

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DictationStore(context: Context) {
    private val prefs = context.getSharedPreferences("veya_dictations", Context.MODE_PRIVATE)
    fun read(): JSONArray = try { JSONArray(prefs.getString("history", "[]")) } catch (_: Exception) { JSONArray() }
    fun save(text: String): Boolean {
        if (text.isBlank()) return true
        val old = read()
        val next = JSONArray().put(JSONObject().put("text", text.trim()).put("createdAt", System.currentTimeMillis()))
        for (i in 0 until minOf(old.length(), 99)) next.put(old.getJSONObject(i))
        return prefs.edit().putString("history", next.toString()).commit()
    }
    fun clear(): Boolean = prefs.edit().remove("history").commit()
}
