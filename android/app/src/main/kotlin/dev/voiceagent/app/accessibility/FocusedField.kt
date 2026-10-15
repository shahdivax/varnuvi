package dev.voiceagent.app.accessibility

import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides if a focused node may get the bubble. Looks only at flags and
 * input type, never at the node's text.
 */
object FocusedField {
    fun isEligible(node: AccessibilityNodeInfo): Boolean =
        node.isEditable && node.isEnabled && node.isVisibleToUser && !isSensitive(node)

    fun isSensitive(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        // Variation values overlap across input classes (URI text is 0x10 too).
        return when (node.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
            InputType.TYPE_CLASS_TEXT -> variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
            else -> false
        }
    }
}
