package dev.voiceagent.app.overlay

import android.view.WindowInsets
import android.view.WindowManager

object BubblePlacement {
    fun clamp(wm: WindowManager, params: WindowManager.LayoutParams, width: Int, height: Int) {
        val metrics = wm.currentWindowMetrics
        val bounds = metrics.bounds
        val bars = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
        val keyboard = metrics.windowInsets.getInsets(WindowInsets.Type.ime())
        val gap = (12 * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
        val left = bars.left + gap
        val right = (bounds.width() - bars.right - width - gap).coerceAtLeast(left)
        val bottom = (bounds.height() - maxOf(bars.bottom, keyboard.bottom) - height).coerceAtLeast(bars.top)
        params.x = params.x.coerceIn(left, right)
        params.y = params.y.coerceIn(bars.top, bottom)
    }
}
