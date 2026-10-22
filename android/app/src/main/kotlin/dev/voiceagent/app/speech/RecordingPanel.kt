package dev.voiceagent.app.speech

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import dev.voiceagent.app.R

/** A compact recording control: status, real voice activity, cancel, and finish. */
class RecordingPanel(context: Context, dark: Boolean, onClose: () -> Unit) : LinearLayout(context) {
    private val ink = Color.parseColor(if (dark) "#EFF2FF" else "#19213A")
    private val muted = Color.parseColor(if (dark) "#B4BED4" else "#616B83")
    private val accent = Color.parseColor(if (dark) "#B5C4FF" else "#3156D3")
    private val errorColor = Color.parseColor(if (dark) "#FFB0B7" else "#AA394A")
    private val surface = Color.parseColor(if (dark) "#192236" else "#FFF8E7")
    private val tonal = Color.parseColor(if (dark) "#293650" else "#F1E7D4")
    private val floatingBorder = Color.parseColor(if (dark) "#62718D" else "#BAAA8D")
    val title: TextView
    val timer: TextView
    val detail: TextView
    val action: ImageButton
    val partial: Button
    val meter: VoiceLevelView
    private val progress: ProgressBar
    private val controls: LinearLayout
    fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    fun place(window: Window, keyboardTop: Int = -1, micCenterX: Int = -1, micCenterY: Int = -1) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        val metrics = window.windowManager.currentWindowMetrics
        val navigation = metrics.windowInsets.getInsets(WindowInsets.Type.navigationBars()).bottom
        val keyboard = if (keyboardTop in 1 until metrics.bounds.height())
            (metrics.bounds.height() - keyboardTop - navigation).coerceAtLeast(0) else 0
        window.setLayout(minOf((metrics.bounds.width() * .92).toInt(), dp(280)), LayoutParams.WRAP_CONTENT)
        if (micCenterX >= 0 && micCenterY >= 0) {
            // Bubble coordinates are absolute screen coordinates, including the status-bar area.
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            fun anchor() {
                if (height == 0 || controls.height == 0) return
                val screen = window.windowManager.currentWindowMetrics
                val bars = screen.windowInsets.getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                val left = bars.left + dp(8)
                val top = bars.top + dp(8)
                val x = (micCenterX - width / 2).coerceIn(left,
                    (screen.bounds.width() - bars.right - width - dp(8)).coerceAtLeast(left))
                val y = (micCenterY - controls.top - controls.height / 2).coerceIn(top,
                    (screen.bounds.height() - bars.bottom - height - dp(8)).coerceAtLeast(top))
                val rootInWindow = IntArray(2)
                val controlsInWindow = IntArray(2)
                getLocationInWindow(rootInWindow)
                controls.getLocationInWindow(controlsInWindow)
                val windowX = x - rootInWindow[0]
                val windowY = y - (controlsInWindow[1] - controls.top)
                val params = window.attributes
                if (params.x != windowX || params.y != windowY) {
                    params.x = windowX; params.y = windowY
                    window.attributes = params
                }
            }
            window.attributes = window.attributes.apply {
                gravity = Gravity.TOP or Gravity.LEFT
                setFitInsetsTypes(0)
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                dimAmount = 0f
            }
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> anchor() }
            post { anchor() }
        } else {
            window.attributes = window.attributes.apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                y = keyboard + dp(16)
                dimAmount = 0f
            }
        }
    }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (color == surface) setStroke(dp(1).coerceAtLeast(1), floatingBorder)
    }
    private fun text(value: String, size: Float, color: Int = ink) = TextView(context).apply {
        text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        includeFontPadding = false
    }
    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(8), dp(8), dp(8), dp(12))
        val status = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            background = shape(surface, 20)
            setPadding(dp(14), dp(7), dp(14), dp(7))
        }
        title = text("Getting ready", 13f).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        timer = text("00:00", 13f, muted).apply {
            fontFeatureSettings = "tnum"
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        status.addView(title)
        status.addView(timer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = dp(10) })
        addView(status, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        controls = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = shape(surface, 36).apply {
                setStroke(dp(1).coerceAtLeast(1), floatingBorder)
            }
            elevation = 0f
        }
        controls.addView(ImageButton(context).apply {
            setImageDrawable(RecordingGlyph(context, "cancel", ink))
            imageTintList = ColorStateList.valueOf(ink)
            background = RippleDrawable(ColorStateList.valueOf(tonal), shape(tonal, 24), null)
            contentDescription = "Cancel dictation"; tooltipText = "Cancel"
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setOnClickListener { onClose() }
        }, LayoutParams(dp(48), dp(48)))
        val activity = FrameLayout(context).apply { background = shape(tonal, 24) }
        meter = VoiceLevelView(context, accent, muted)
        activity.addView(meter, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(36), Gravity.CENTER))
        progress = ProgressBar(context).apply {
            indeterminateTintList = ColorStateList.valueOf(accent)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            visibility = View.GONE
        }
        activity.addView(progress, FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER))
        controls.addView(activity, LayoutParams(0, dp(48), 1f).apply { marginStart = dp(10); marginEnd = dp(10) })
        action = ImageButton(context).apply {
            setImageDrawable(RecordingGlyph(context, "done", if (dark) Color.parseColor("#192A5D") else Color.parseColor("#FFF8E7")))
            isEnabled = false
            contentDescription = "Finish dictation"; tooltipText = "Done"
            setPadding(dp(13), dp(13), dp(13), dp(13))
            background = RippleDrawable(ColorStateList.valueOf(tonal), shape(accent, 24), null)
            backgroundTintList = null
            stateListAnimator = null; elevation = 0f
        }
        controls.addView(action, LayoutParams(dp(48), dp(48)))
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, dp(68)).apply { topMargin = dp(8) })
        detail = text("", 13f, muted).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            background = shape(surface, 16)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setLineSpacing(dp(3).toFloat(), 1f)
            visibility = View.GONE
        }
        addView(detail, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
        partial = Button(context).apply {
            text = "Use completed text"; textSize = 13f; isAllCaps = false
            setTextColor(accent); background = shape(surface, 24); visibility = View.GONE
            minHeight = dp(48); minimumHeight = dp(48)
            stateListAnimator = null; elevation = 0f; backgroundTintList = null
        }
        addView(partial, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })
    }
    fun setBusy(busy: Boolean, retry: Boolean) {
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        meter.visibility = if (busy) View.INVISIBLE else View.VISIBLE
        val iconColor = if (Color.luminance(accent) > .4) Color.parseColor("#192A5D") else Color.parseColor("#FFF8E7")
        if (retry) action.setImageResource(R.drawable.ic_retry)
        else action.setImageDrawable(RecordingGlyph(context, "done", iconColor))
        action.imageTintList = ColorStateList.valueOf(iconColor)
        action.contentDescription = if (retry) "Try again" else "Finish dictation"
        action.tooltipText = if (retry) "Try again" else "Done"
        action.alpha = if (action.isEnabled) 1f else .45f
        timer.visibility = if (busy || retry) View.GONE else View.VISIBLE
    }
    fun setError(failed: Boolean) {
        detail.setTextColor(if (failed) errorColor else muted)
    }
}

/** Rolling measured RMS input. Silence stays flat; no simulated waveform. */
class VoiceLevelView(context: Context, private val accent: Int, private val baseline: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val levels = FloatArray(17)
    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
    fun push(level: Float) {
        levels.copyInto(levels, 0, 1, levels.size)
        levels[levels.lastIndex] = level.coerceIn(0f, 1f)
        invalidate()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val spacing = minOf(width / 21f, 6f * density)
        val barWidth = minOf(3f * density, spacing * .55f)
        val start = (width - spacing * (levels.size - 1)) / 2
        for (i in levels.indices) {
            val value = levels[i]
            val barHeight = 3f * density + (height - 8f * density).coerceAtLeast(0f) * value
            paint.color = if (value < .015f) baseline else accent
            paint.alpha = if (value < .015f) 130 else (130 + 125 * (i + 1) / levels.size)
            val x = start + i * spacing
            canvas.drawRoundRect(x - barWidth / 2, (height - barHeight) / 2, x + barWidth / 2, (height + barHeight) / 2, barWidth / 2, barWidth / 2, paint)
        }
    }
}

/** Consistent stroked controls, independent of system icon/font substitutions. */
private class RecordingGlyph(context: Context, private val kind: String, color: Int) : Drawable() {
    private val size = (24 * context.resources.displayMetrics.density).toInt()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; style = Paint.Style.STROKE; strokeWidth = 2.1f
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    override fun getIntrinsicWidth() = size
    override fun getIntrinsicHeight() = size
    override fun draw(canvas: Canvas) {
        canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        val path = Path()
        when (kind) {
            "cancel" -> { path.moveTo(6f, 6f); path.lineTo(18f, 18f); path.moveTo(18f, 6f); path.lineTo(6f, 18f) }
            else -> { path.moveTo(5f, 12f); path.lineTo(10f, 17f); path.lineTo(19f, 7f) }
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Drawable compatibility") override fun getOpacity() = PixelFormat.TRANSLUCENT
}
