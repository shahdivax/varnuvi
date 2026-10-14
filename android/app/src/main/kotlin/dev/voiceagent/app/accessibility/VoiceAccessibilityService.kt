package dev.voiceagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.voiceagent.app.allowlist.AllowlistStore
import dev.voiceagent.app.overlay.BubbleOverlay

/**
 * Shows the bubble while an editable, non-password field is focused in an
 * enabled app. Looks only at the focused node's flags and the package name;
 * it never reads, logs, stores, or transmits any on-screen text.
 */
class VoiceAccessibilityService : AccessibilityService() {
    private var allowlist: AllowlistStore? = null
    private var bubble: BubbleOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        allowlist = AllowlistStore(this)
        bubble = BubbleOverlay(this) { Log.d(TAG, "Bubble tapped") }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val type = event?.eventType ?: return
        if (type != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return
        refresh()
    }

    // Decide from the active window, not the event source: the bubble's own
    // window emits events too, and must not hide itself.
    private fun refresh() {
        val root = rootInActiveWindow
        val pkg = root?.packageName?.toString()
        val focused = root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val show = pkg != null && pkg != packageName &&
            allowlist?.isEnabled(pkg) == true &&
            focused != null && FocusedField.isEligible(focused)
        if (show) {
            if (bubble?.isShowing == false) Log.d(TAG, "Bubble shown in $pkg")
            bubble?.show()
        } else {
            bubble?.hide()
        }
    }

    override fun onInterrupt() {
        bubble?.hide()
    }

    override fun onDestroy() {
        bubble?.hide()
        bubble = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "VoiceA11y"
    }
}
