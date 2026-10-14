package dev.voiceagent.app.overlay

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import dev.voiceagent.app.R

/**
 * Applies a [BubbleState] to the bubble: tone, glyph, label, subtle motion.
 * Listening shrinks-and-returns (never grows, so the window never clips);
 * processing shows dots with a soft fade.
 */
class BubbleStateRenderer(private val view: ImageView) {
    private var anim: ValueAnimator? = null

    fun render(state: BubbleState) {
        stop()
        val ctx = view.context
        val bg = view.background as? GradientDrawable
        when (state) {
            BubbleState.IDLE -> {
                bg?.setColor(ctx.getColor(R.color.bubble_background))
                view.setImageResource(R.drawable.ic_bubble_mic)
                view.contentDescription = ctx.getString(R.string.bubble_content_description)
            }
            BubbleState.LISTENING -> {
                bg?.setColor(ctx.getColor(R.color.bubble_active))
                view.setImageResource(R.drawable.ic_bubble_mic)
                view.contentDescription = ctx.getString(R.string.bubble_listening_description)
                anim = pulse(
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f),
                )
            }
            BubbleState.PROCESSING -> {
                bg?.setColor(ctx.getColor(R.color.bubble_background))
                view.setImageResource(R.drawable.ic_bubble_dots)
                view.contentDescription = ctx.getString(R.string.bubble_processing_description)
                anim = pulse(PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.55f))
            }
        }
    }

    /** Ends any motion and resets the view to rest. */
    fun stop() {
        anim?.cancel()
        anim = null
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
    }

    // Honors the system animator duration scale ("Remove animations").
    private fun pulse(vararg values: PropertyValuesHolder): ValueAnimator =
        ObjectAnimator.ofPropertyValuesHolder(view, *values).apply {
            duration = PULSE_MS
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }

    companion object {
        private const val PULSE_MS = 700L
    }
}
