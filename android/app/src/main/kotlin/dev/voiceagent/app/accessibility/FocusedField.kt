package dev.voiceagent.app.accessibility

import android.text.InputType
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides if a focused node may get the bubble. Looks only at flags and
 * input type, never at the node's text.
 */
object FocusedField {
    private val SENSITIVE_VARIATIONS = setOf(
        InputType.TYPE_TEXT_VARIATION_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
        InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
        InputType.TYPE_NUMBER_VARIATION_PASSWORD,
    )

    fun isEligible(node: AccessibilityNodeInfo): Boolean =
        node.isEditable && !isSensitive(node)

    fun isSensitive(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val variation = node.inputType and InputType.TYPE_MASK_VARIATION
        return variation in SENSITIVE_VARIATIONS
    }
}
