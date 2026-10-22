package dev.voiceagent.app.speech

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

/** Quiet, editable result. Clipboard changes only after an explicit tap. */
class TranscriptPanel(context: Context, dark: Boolean, text: String, saved: Boolean,
    onClose: () -> Unit, onCopy: (String) -> Unit) : LinearLayout(context) {
    val editor: EditText
    init {
        fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
        val ink = Color.parseColor(if (dark) "#EFF2FF" else "#19213A")
        val muted = Color.parseColor(if (dark) "#B4BED4" else "#616B83")
        val accent = Color.parseColor(if (dark) "#B5C4FF" else "#3156D3")
        val surface = Color.parseColor(if (dark) "#192236" else "#FFF8E7")
        fun shape(color: Int, radius: Int) = GradientDrawable().apply {
            setColor(color); cornerRadius = dp(radius).toFloat()
        }
        orientation = VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = shape(surface, 28).apply {
            setStroke(dp(1).coerceAtLeast(1), Color.parseColor(if (dark) "#62718D" else "#BAAA8D"))
        }
        val heading = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        heading.addView(TextView(context).apply {
            this.text = "Your words, ready."; textSize = 19f; setTextColor(ink)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        heading.addView(Button(context).apply {
            this.text = "Close"; isAllCaps = false; textSize = 13f; setTextColor(muted)
            background = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(if (dark) Color.parseColor("#293650") else Color.parseColor("#F1E7D4")),
                shape(Color.TRANSPARENT, 24), null)
            backgroundTintList = null; stateListAnimator = null; elevation = 0f
            minWidth = dp(48); minimumWidth = dp(48); minHeight = dp(48)
            setPadding(dp(8), 0, 0, 0); setOnClickListener { onClose() }
        }, LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)))
        addView(heading)
        editor = EditText(context).apply {
            setText(text); textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            gravity = Gravity.TOP; minLines = 3; maxLines = 7
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(if (dark) Color.parseColor("#293650") else Color.parseColor("#F1E7D4")),
                shape(Color.TRANSPARENT, 24), null)
            backgroundTintList = null; stateListAnimator = null; elevation = 0f
            setPadding(0, dp(12), 0, dp(16)); setLineSpacing(dp(4).toFloat(), 1f)
            contentDescription = "Transcript. Edit before copying."
        }
        addView(editor, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(TextView(context).apply {
            this.text = if (saved) "Saved to History · Paste wherever you need." else "History couldn't be saved. Copy your words before closing."
            textSize = 12f; setTextColor(muted); setLineSpacing(dp(3).toFloat(), 1f)
        })
        addView(Button(context).apply {
            this.text = "Copy & close"; isAllCaps = false; textSize = 15f
            setTextColor(if (dark) Color.parseColor("#192A5D") else surface)
            background = shape(accent, 28); backgroundTintList = null
            stateListAnimator = null; elevation = 0f
            setOnClickListener {
                val value = editor.text.toString()
                if (value.isNotBlank()) onCopy(value)
                else editor.error = "Add some text to copy."
            }
        }, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(18) })
        // Keep the keyboard closed until the user explicitly chooses to edit.
        isFocusableInTouchMode = true
        requestFocus()
    }
}
