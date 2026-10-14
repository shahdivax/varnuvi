package dev.voiceagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.voiceagent.app.allowlist.AllowlistStore
import dev.voiceagent.app.insert.MockText
import dev.voiceagent.app.insert.TextInserter
import dev.voiceagent.app.overlay.BubbleOverlay

/**
 * Shows the bubble while an editable, non-password field is focused in an
 * enabled app. Looks only at the focused node's flags and the package name.
 * On a bubble tap it reads the field's text in memory solely to merge the
 * insertion; it never logs, stores, or transmits any on-screen text.
 */
class VoiceAccessibilityService : AccessibilityService() {
    private var allowlist: AllowlistStore? = null
    private var bubble: BubbleOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        allowlist = AllowlistStore(this)
        bubble = BubbleOverlay(this) { insertSample() }
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

    // Mock insertion: a fixed sample sentence, no speech recognition yet.
    // Re-checks the field at tap time; logs only the path and package.
    private fun insertSample() {
        val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val pkg = focused?.packageName?.toString()
        if (focused == null || pkg == null || pkg == packageName ||
            allowlist?.isEnabled(pkg) != true || !FocusedField.isEligible(focused)
        ) {
            Log.d(TAG, "Insert skipped: no eligible field")
            return
        }
        val path = TextInserter.insert(this, focused, MockText.SAMPLE_SENTENCE)
        Log.d(TAG, "Insert path=$path in $pkg")
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
