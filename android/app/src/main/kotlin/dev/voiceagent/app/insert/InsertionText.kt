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
     * The field's real content. Many messaging apps (WhatsApp, Samsung Messages,
     * Telegram, Instagram...) expose their placeholder ("Message", "Text message")
     * as the node text without flagging it as a hint. Text is treated as empty when:
     *  - Android says the hint is showing, or the text equals the reported hint; or
     *  - the app reports no usable hint, the cursor sits at the very start, and the
     *    text is a short single line. A typed draft leaves the cursor after the text,
     *    so this matches an untouched placeholder, not the user's own words.
     */
    fun visible(text: CharSequence?, hint: CharSequence?, showingHint: Boolean,
                start: Int = -1, end: Int = -1): String {
        val value = text?.toString().orEmpty()
        val placeholder = hint?.toString()
        if (showingHint || value.isEmpty()) return ""
        if (!placeholder.isNullOrEmpty()) return if (value == placeholder) "" else value
        val cursorAtStart = start <= 0 && end <= 0
        val shortLine = value.length <= MAX_PLACEHOLDER && '\n' !in value
        return if (cursorAtStart && shortLine) "" else value
    }

    private const val MAX_PLACEHOLDER = 40

    fun merge(current: String, start: Int, end: Int, addition: String): Edit {
        val a = start.takeIf { it in 0..current.length } ?: current.length
        val b = end.takeIf { it in 0..current.length } ?: a
        val left = minOf(a, b)
        val right = maxOf(a, b)
        return Edit(current.substring(0, left) + addition + current.substring(right), left + addition.length)
    }
}
