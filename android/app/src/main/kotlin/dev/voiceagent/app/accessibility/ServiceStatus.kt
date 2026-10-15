package dev.voiceagent.app.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Reads whether our accessibility service is on and opens system settings. */
object ServiceStatus {
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, VoiceAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }

    fun openSettings(context: Context) {
        // The per-service details intent requires a privileged permission on Pixel.
        // Use the public settings entry point available to ordinary apps.
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
