package dev.voiceagent.app.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Plain class, not a data class: data-class equals() on ByteArray compares identity. */
class InstalledApp(val name: String, val packageName: String, val icon: ByteArray)

/** Lists launchable apps (name, package, icon PNG), excluding this app. */
object InstalledApps {
    fun launchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map {
                InstalledApp(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = AppIconEncoder.toPng(it.loadIcon(pm)),
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
