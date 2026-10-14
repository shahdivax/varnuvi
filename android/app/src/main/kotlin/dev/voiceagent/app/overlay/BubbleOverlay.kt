package dev.voiceagent.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Floating bubble in a TYPE_ACCESSIBILITY_OVERLAY window, so no separate
 * overlay permission is needed. Must be created with the service context.
 * Not focusable: it never steals focus from the field being typed in.
 */
class BubbleOverlay(private val context: Context, private val onTap: () -> Unit) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private val positions = BubblePositionStore(context)
    private var view: View? = null

    val isShowing: Boolean get() = view != null

    fun show() {
        if (view != null) return
        val size = (BubbleView.SIZE_DP * context.resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        // Saved spot, clamped in case the screen size changed; default is the
        // right edge at 40% height.
        val bounds = wm.currentWindowMetrics.bounds
        val maxX = (bounds.width() - size).coerceAtLeast(0)
        val maxY = (bounds.height() - size).coerceAtLeast(0)
        val saved = positions.load()
        params.x = (saved?.x ?: maxX).coerceIn(0, maxX)
        params.y = (saved?.y ?: (bounds.height() * 2 / 5)).coerceIn(0, maxY)

        val bubble = BubbleView.create(context).apply {
            setOnClickListener { onTap() }
            setOnTouchListener(BubbleTouchListener(wm, params) { x, y -> positions.save(x, y) })
        }
        wm.addView(bubble, params)
        view = bubble
    }

    fun hide() {
        val v = view ?: return
        wm.removeView(v)
        view = null
    }
}
