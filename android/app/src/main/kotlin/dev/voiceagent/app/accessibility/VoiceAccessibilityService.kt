package dev.voiceagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Skeleton service. Logs only the package name when an editable,
 * non-password field is focused. It never reads, logs, stores, or
 * transmits any on-screen text.
 */
class VoiceAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        val focused = rootInActiveWindow
            ?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        if (FocusedField.isEligible(focused)) {
            Log.d(TAG, "Editable field focused in $pkg")
        }
    }

    override fun onInterrupt() = Unit

    companion object {
        private const val TAG = "VoiceA11y"
    }
}
