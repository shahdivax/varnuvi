package dev.voiceagent.app.insert

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Inserts text into the focused field. Tries ACTION_SET_TEXT merged at the
 * cursor first, then falls back to clipboard + ACTION_PASTE. The field's
 * current text is used only in memory to merge; it is never logged or stored.
 */
object TextInserter {
    enum class Path { SET_TEXT, PASTE, FAILED }

    fun insert(context: Context, node: AccessibilityNodeInfo, text: String): Path {
        if (setText(node, text)) return Path.SET_TEXT
        if (paste(context, node, text)) return Path.PASTE
        return Path.FAILED
    }

    private fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        // A hint is not real content; treat the field as empty.
        val current = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        // No cursor info (-1) means append at the end.
        val start = node.textSelectionStart.takeIf { it in 0..current.length } ?: current.length
        val end = node.textSelectionEnd.takeIf { it in start..current.length } ?: start
        val merged = current.substring(0, start) + text + current.substring(end)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged)
        }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return false
        val cursor = start + text.length
        val sel = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, sel)
        return true
    }

    private fun paste(context: Context, node: AccessibilityNodeInfo, text: String): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("dictation", text))
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }
}
