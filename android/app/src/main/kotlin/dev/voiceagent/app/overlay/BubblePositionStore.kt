package dev.voiceagent.app.overlay

import android.content.Context
import android.graphics.Point

/** Remembers where the user last dropped the bubble (screen pixels). */
class BubblePositionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): Point? =
        if (prefs.contains(KEY_X)) Point(prefs.getInt(KEY_X, 0), prefs.getInt(KEY_Y, 0)) else null

    fun save(x: Int, y: Int) {
        prefs.edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply()
    }

    companion object {
        private const val PREFS = "bubble"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
    }
}
