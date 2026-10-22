package dev.voiceagent.app.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.widget.ImageView
import dev.voiceagent.app.R

/** Compact branded dictation button. */
object BubbleView {
    /** A 44 dp circle inside a 52 dp touch target. */
    const val SIZE_DP = 52

    fun create(context: Context): ImageView {
        val density = context.resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(context.getColor(R.color.bubble_background))
            setStroke(density.toInt().coerceAtLeast(1), context.getColor(R.color.bubble_border))
        }
        return ImageView(context).apply {
            background = InsetDrawable(bg, (4 * density).toInt())
            setImageResource(R.drawable.varnuvi_mark)
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.bubble_foreground))
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            contentDescription = context.getString(R.string.bubble_content_description)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
    }
}
