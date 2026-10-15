package dev.voiceagent.app.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.widget.ImageView
import dev.voiceagent.app.R

/** Builds the round bubble: tonal circle, thin border, mic glyph. */
object BubbleView {
    /** A 40 dp circle inside a comfortable 48 dp touch target. */
    const val SIZE_DP = 48

    fun create(context: Context): ImageView {
        val density = context.resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(context.getColor(R.color.bubble_background))
            setStroke(density.toInt().coerceAtLeast(1), context.getColor(R.color.bubble_border))
        }
        return ImageView(context).apply {
            background = InsetDrawable(bg, (4 * density).toInt())
            setImageResource(R.drawable.ic_bubble_mic)
            val pad = (14 * density).toInt()
            setPadding(pad, pad, pad, pad)
            contentDescription = context.getString(R.string.bubble_content_description)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }
}
