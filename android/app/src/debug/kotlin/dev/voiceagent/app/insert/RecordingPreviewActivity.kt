package dev.voiceagent.app.insert

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import dev.voiceagent.app.R
import dev.voiceagent.app.speech.TranscriptPanel

/** Synthetic overlay result fixture: no recording, network, or real clipboard changes. */
class RecordingPreviewActivity : Activity() {
    lateinit var panel: TranscriptPanel
    var copied: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = intent.getBooleanExtra("dark", false)
        setTheme(if (dark) R.style.RecordingThemeDark else R.style.RecordingThemeLight)
        super.onCreate(savedInstanceState)
        panel = TranscriptPanel(this, dark,
            "A little more room for your words.\n\nSpeak naturally. Copy when you're ready.", true,
            { finish() }, { copied = it })
        setContentView(android.widget.ScrollView(this).apply { addView(panel) })
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        window.setLayout(minOf((resources.displayMetrics.widthPixels * .92).toInt(),
            (360 * resources.displayMetrics.density).toInt()), ViewGroup.LayoutParams.WRAP_CONTENT)
        window.attributes = window.attributes.apply { gravity = Gravity.CENTER }
    }
}

/** Opaque host keeps synthetic screenshots free of the user's underlying apps. */
class PreviewBackdropActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#E5E9F1")))
        setContentView(android.widget.TextView(this).apply {
            text = "Varnuvi · preview"; textSize = 16f
            setTextColor(Color.parseColor("#616B83"))
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            setPadding(0, (40 * resources.displayMetrics.density).toInt(), 0, 0)
        })
    }
}
