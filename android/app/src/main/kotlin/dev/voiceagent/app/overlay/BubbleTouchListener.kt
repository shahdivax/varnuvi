package dev.voiceagent.app.overlay

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.hypot

/** Drags the bubble window. A touch that never moves past touch slop is a tap. */
class BubbleTouchListener(
    private val wm: WindowManager,
    private val params: WindowManager.LayoutParams,
    private val onMoved: (x: Int, y: Int) -> Unit,
) : View.OnTouchListener {
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                startX = params.x
                startY = params.y
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.rawX - downX
                val dy = e.rawY - downY
                val slop = ViewConfiguration.get(v.context).scaledTouchSlop
                if (!dragging && hypot(dx, dy) > slop) dragging = true
                if (dragging) move(v, startX + dx.toInt(), startY + dy.toInt())
            }
            MotionEvent.ACTION_UP -> if (dragging) onMoved(params.x, params.y) else v.performClick()
            MotionEvent.ACTION_CANCEL -> if (dragging) onMoved(params.x, params.y)
        }
        return true
    }

    private fun move(v: View, x: Int, y: Int) {
        if (!v.isAttachedToWindow) return
        params.x = x
        params.y = y
        BubblePlacement.clamp(wm, params, v.width, v.height)
        try { wm.updateViewLayout(v, params) } catch (_: IllegalArgumentException) { /* App changed during drag. */ }
    }
}
