package dev.voiceagent.app.insert

/** Pure cursor merge, using Android's UTF-16 selection offsets. */
object InsertionText {
    data class Edit(val text: String, val cursor: Int)

    /**
     * The field's real content. Some apps (e.g. Samsung Messages) expose their
     * placeholder ("Text message") as the node text without flagging it as a
     * hint, so text equal to the hint is treated as empty.
     */
    fun visible(text: CharSequence?, hint: CharSequence?, showingHint: Boolean): String {
        val value = text?.toString().orEmpty()
        val placeholder = hint?.toString()
        return if (showingHint || (!placeholder.isNullOrEmpty() && value == placeholder)) "" else value
    }
    fun merge(current: String, start: Int, end: Int, addition: String): Edit {
        val a = start.takeIf { it in 0..current.length } ?: current.length
        val b = end.takeIf { it in 0..current.length } ?: a
        val left = minOf(a, b)
        val right = maxOf(a, b)
        return Edit(current.substring(0, left) + addition + current.substring(right), left + addition.length)
    }
}
