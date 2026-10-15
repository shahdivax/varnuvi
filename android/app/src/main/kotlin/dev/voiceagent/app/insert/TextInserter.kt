package dev.voiceagent.app.insert

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

object TextInserter {
    enum class Path { SET_TEXT, PASTE, FAILED }
    data class Result(val path: Path, val expectedText: String)

    fun insert(context: Context, node: AccessibilityNodeInfo, text: String): Result {
        val current = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
        val start = node.textSelectionStart
        val end = node.textSelectionEnd
        val edit = InsertionText.merge(current, start, end, text)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, edit.text)
        }
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, edit.cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, edit.cursor)
            })
            return Result(Path.SET_TEXT, edit.text)
        }
        // Do not duplicate text if an app applied the action but returned false.
        if (node.refresh() && node.text?.toString() == edit.text) return Result(Path.SET_TEXT, edit.text)
        // A failed action may change focus/content. Never paste into that changed field.
        if (!node.refresh() || !node.isFocused || node.textSelectionStart != start || node.textSelectionEnd != end || (if (node.isShowingHintText) "" else node.text?.toString().orEmpty()) != current) return Result(Path.FAILED, edit.text)
        return Result(if (paste(context, node, text)) Path.PASTE else Path.FAILED, edit.text)
    }
    private fun paste(context: Context, node: AccessibilityNodeInfo, text: String): Boolean {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        return try {
            val previous = clipboard.primaryClip
            clipboard.setPrimaryClip(ClipData.newPlainText("Varnuvi dictation", text))
            val writtenAt = clipboard.primaryClipDescription?.timestamp
            val pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val clip = clipboard.primaryClip
                    // Restore only our clipboard item, never a newer user copy.
                    if (writtenAt != null && clip?.description?.timestamp == writtenAt && clip.description.label?.toString() == "Varnuvi dictation" && clip.itemCount == 1 && clip.getItemAt(0).text?.toString() == text) {
                        if (previous != null) clipboard.setPrimaryClip(previous) else clipboard.clearPrimaryClip()
                    }
                } catch (_: SecurityException) { /* Some apps disallow clipboard access. */ }
            }, if (pasted) 1000 else 0)
            pasted
        } catch (_: SecurityException) { false }
    }
}
