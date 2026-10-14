package dev.voiceagent.app.overlay

import android.os.Handler
import android.os.Looper

/**
 * Fake dictation cycle on the bubble: idle -> listening -> processing ->
 * insert -> idle. Stands in for real speech recognition (out of scope).
 * Timings mirror lib/mock/fake_dictation.dart.
 */
class BubbleDictation(
    private val onState: (BubbleState) -> Unit,
    private val onDone: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    var state = BubbleState.IDLE
        private set

    /** Tap: start when idle, stop listening early, ignore while processing. */
    fun toggle() {
        when (state) {
            BubbleState.IDLE -> go(BubbleState.LISTENING, LISTEN_MS) { process() }
            BubbleState.LISTENING -> process()
            BubbleState.PROCESSING -> Unit
        }
    }

    /** Abort without inserting, e.g. when the field loses focus. */
    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        set(BubbleState.IDLE)
    }

    private fun process() = go(BubbleState.PROCESSING, PROCESS_MS) {
        set(BubbleState.IDLE)
        onDone()
    }

    private fun go(next: BubbleState, afterMs: Long, then: () -> Unit) {
        handler.removeCallbacksAndMessages(null)
        set(next)
        handler.postDelayed(then, afterMs)
    }

    private fun set(next: BubbleState) {
        if (state == next) return
        state = next
        onState(next)
    }

    companion object {
        const val LISTEN_MS = 3000L
        const val PROCESS_MS = 1200L
    }
}
