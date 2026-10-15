package dev.voiceagent.app.speech

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import dev.voiceagent.app.R

/** The recorder's native surface, shared by the app and floating bubble. */
class RecordingPanel(context: Context, dark: Boolean, onClose: () -> Unit) : LinearLayout(context) {
    private val ink = Color.parseColor(if (dark) "#EFF2FF" else "#19213A")
    private val muted = Color.parseColor(if (dark) "#A5B1CD" else "#616B83")
    private val accent = Color.parseColor(if (dark) "#A9BBFF" else "#3156D3")
    private val errorColor = Color.parseColor(if (dark) "#FFB0B7" else "#AA394A")
    private val surface = Color.parseColor(if (dark) "#1A2133" else "#EDF0F8")
    val title: TextView
    val timer: TextView
    val detail: TextView
    val action: Button
    val partial: Button
    val meter: VoiceLevelView
    private val progress: ProgressBar
    fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun text(value: String, size: Float, color: Int = ink) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        includeFontPadding = false
    }
    private fun gap(size: Int) { addView(View(context), LayoutParams(1, dp(size))) }
    init {
        orientation = VERTICAL
        setPadding(dp(24), dp(20), dp(24), dp(24))
        background = shape(Color.parseColor(if (dark) "#111624" else "#F7F8FC"), 32)
        clipToOutline = true
        val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(ImageView(context).apply {
            setImageResource(R.drawable.varnuvi_mark); imageTintList = ColorStateList.valueOf(accent)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(dp(30), dp(30)))
        header.addView(text("varnuvi", 22f).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); letterSpacing = -.04f
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), 0, 0, 0)
        }, LayoutParams(0, dp(48), 1f))
        header.addView(ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(muted)
            background = RippleDrawable(ColorStateList.valueOf(surface), null, shape(Color.WHITE, 24))
            contentDescription = "Discard recording"; tooltipText = "Discard recording"
            setPadding(dp(14), dp(14), dp(14), dp(14)); setOnClickListener { onClose() }
        }, LayoutParams(dp(48), dp(48)))
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        gap(26)
        title = text("Ready to record", 16f, muted)
        title.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        addView(title, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        gap(12)
        timer = text("00:00", 58f).apply {
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = -.045f; fontFeatureSettings = "tnum"
        }
        addView(timer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        gap(10)
        addView(text("30-minute session limit", 13f, muted))
        gap(18)
        meter = VoiceLevelView(context, accent, surface)
        addView(meter, LayoutParams(LayoutParams.MATCH_PARENT, dp(78)))
        gap(20)
        val work = LinearLayout(context).apply {
            orientation = VERTICAL; background = shape(surface, 18); setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        detail = text("Transcription starts at 0:30", 14f, muted).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        work.addView(detail, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1800; progressTintList = ColorStateList.valueOf(accent)
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor(if (dark) "#35405A" else "#D5DCEC"))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        work.addView(progress, LayoutParams(LayoutParams.MATCH_PARENT, dp(5)).apply { topMargin = dp(12) })
        addView(work, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        gap(22)
        val disabled = Color.parseColor(if (dark) "#242D43" else "#DFE6FF")
        val buttonColors = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(disabled, accent))
        action = Button(context).apply {
            text = "Submit"; textSize = 16f; isAllCaps = false; isEnabled = false
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = RippleDrawable(ColorStateList.valueOf(Color.parseColor("#334D67C8")), shape(accent, 28), null)
            backgroundTintList = buttonColors
            setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(muted, Color.parseColor(if (dark) "#13255B" else "#FFFFFF"))))
            stateListAnimator = null; minHeight = dp(56); elevation = 0f; letterSpacing = 0f
        }
        addView(action, LayoutParams(LayoutParams.MATCH_PARENT, dp(56)))
        partial = Button(context).apply {
            text = "Use completed text"; textSize = 14f; isAllCaps = false
            setTextColor(accent); background = null; visibility = View.GONE
        }
        addView(partial, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(8) })
    }
    fun setProgress(completed: Int, total: Int, failed: Boolean) {
        progress.max = total.coerceAtLeast(1)
        progress.progress = completed.coerceIn(0, progress.max)
        detail.setTextColor(if (failed) errorColor else muted)
    }
}

/** A rolling history of measured RMS input. Silence stays flat; no simulated waveform. */
class VoiceLevelView(context: Context, private val accent: Int, private val baseline: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val levels = FloatArray(33)
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun push(level: Float) {
        levels.copyInto(levels, 0, 1, levels.size)
        levels[levels.lastIndex] = level.coerceIn(0f, 1f)
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val spacing = minOf(width / 36f, 9f * density)
        val barWidth = minOf(4f * density, spacing * .5f)
        val start = (width - spacing * (levels.size - 1)) / 2
        for (i in levels.indices) {
            val value = levels[i]
            val barHeight = 5f * density + (height - 12f * density).coerceAtLeast(0f) * value
            paint.color = if (value < .015f) baseline else accent
            paint.alpha = if (value < .015f) 255 else (100 + 155 * (i + 1) / levels.size)
            val x = start + i * spacing
            canvas.drawRoundRect(x - barWidth / 2, (height - barHeight) / 2, x + barWidth / 2, (height + barHeight) / 2, barWidth / 2, barWidth / 2, paint)
        }
    }
}
