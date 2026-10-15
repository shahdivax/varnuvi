package dev.voiceagent.app.insert

/** Pure cursor merge, using Android's UTF-16 selection offsets. */
object InsertionText {
    data class Edit(val text: String, val cursor: Int)
    fun merge(current: String, start: Int, end: Int, addition: String): Edit {
        val a = start.takeIf { it in 0..current.length } ?: current.length
        val b = end.takeIf { it in 0..current.length } ?: a
        val left = minOf(a, b)
        val right = maxOf(a, b)
        return Edit(current.substring(0, left) + addition + current.substring(right), left + addition.length)
    }
}
