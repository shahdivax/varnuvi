package dev.voiceagent.app.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
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
    private var layout: WindowManager.LayoutParams? = null

    val isShowing: Boolean get() = view?.isAttachedToWindow == true

    fun show(anchor: Rect? = null) {
        fun place(params: WindowManager.LayoutParams, width: Int, height: Int) {
            val gap = (12 * context.resources.displayMetrics.density).toInt()
            if (anchor != null) {
                params.x = anchor.centerX() - width / 2
                params.y = anchor.bottom + gap
            }
            BubblePlacement.clamp(wm, params, width, height)
            if (anchor != null && Rect(params.x, params.y, params.x + width, params.y + height).intersect(anchor)) {
                params.y = anchor.top - height - gap
                BubblePlacement.clamp(wm, params, width, height)
            }
        }
        if (view != null && !isShowing) hide()
        if (view != null) {
            val current = view!!
            val params = layout ?: return
            if (anchor != null) current.setOnTouchListener(null)
            val oldX = params.x
            val oldY = params.y
            place(params, current.width, current.height)
            if (oldX != params.x || oldY != params.y) {
                try { wm.updateViewLayout(current, params) } catch (_: IllegalArgumentException) { hide() }
            }
            return
        }
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
        // left edge at 40% height, separate from Android’s right-hand shortcut.
        val bounds = wm.currentWindowMetrics.bounds
        val maxX = (bounds.width() - size).coerceAtLeast(0)
        val maxY = (bounds.height() - size).coerceAtLeast(0)
        val saved = positions.load()
        params.x = (saved?.x ?: 0).coerceIn(0, maxX)
        params.y = (saved?.y ?: (bounds.height() * 2 / 5)).coerceIn(0, maxY)

        val bubble = BubbleView.create(context).apply {
            setOnClickListener { onTap() }
            if (anchor == null) setOnTouchListener(BubbleTouchListener(wm, params) { x, y -> positions.save(x, y) })
        }
        place(params, size, size)
        try { wm.addView(bubble, params) }
        catch (_: WindowManager.BadTokenException) { return }
        catch (_: SecurityException) { return }
        layout = params
        view = bubble
    }

    fun hide() {
        val v = view ?: return
        view = null
        layout = null
        try { wm.removeView(v) } catch (_: IllegalArgumentException) { /* Already detached by Android. */ }
    }
}
