package dev.voiceagent.app.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

data class InstalledApp(val name: String, val packageName: String)

/** Lists launchable apps (name + package), excluding this app. */
object InstalledApps {
    fun launchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }
}
