package dev.voiceagent.app.insert

/** Pure cursor merge, using Android's UTF-16 selection offsets. */
object InsertionText {
    data class Edit(val text: String, val cursor: Int)

    /** Placeholders some apps (e.g. WhatsApp) report as field text with no hint info. */
    private val knownPlaceholders = setOf(
        "message", "text message", "rcs message", "sms message", "chat message",
        "type a message", "type a message…", "type a message...", "write a message",
        "write a message…", "send a message", "aa",
    )

    /**
     * The field's real content. Some apps expose their placeholder ("Text message",
     * "Message") as the node text without flagging it as a hint. Text is treated
     * as empty when it equals the reported hint, or when it is a known placeholder
     * with the cursor at the very start (where a real typed word would not leave it).
     */
    fun visible(text: CharSequence?, hint: CharSequence?, showingHint: Boolean,
                start: Int = -1, end: Int = -1): String {
        val value = text?.toString().orEmpty()
        val placeholder = hint?.toString()
        if (showingHint || value.isEmpty()) return ""
        if (!placeholder.isNullOrEmpty() && value == placeholder) return ""
        val cursorAtStart = start <= 0 && end <= 0
        if (cursorAtStart && value.trim().lowercase() in knownPlaceholders) return ""
        return value
    }

    fun merge(current: String, start: Int, end: Int, addition: String): Edit {
        val a = start.takeIf { it in 0..current.length } ?: current.length
        val b = end.takeIf { it in 0..current.length } ?: a
        val left = minOf(a, b)
        val right = maxOf(a, b)
        return Edit(current.substring(0, left) + addition + current.substring(right), left + addition.length)
    }
}
