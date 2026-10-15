package dev.voiceagent.app.allowlist

import android.content.Context
import android.content.SharedPreferences

/**
 * Package names where the bubble may appear. Empty by default, so the
 * bubble is off in every app until the user enables it.
 */
class AllowlistStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Copy: the set returned by SharedPreferences must not be mutated.
    fun enabledPackages(): Set<String> =
        prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun isEnabled(packageName: String): Boolean = packageName in enabledPackages()

    fun setEnabled(packageName: String, enabled: Boolean) {
        val next = enabledPackages().toMutableSet()
        if (enabled) next.add(packageName) else next.remove(packageName)
        prefs.edit().putStringSet(KEY, next).apply()
    }

    fun addListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun removeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    companion object {
        private const val PREFS = "allowlist"
        private const val KEY = "enabled_packages"
    }
}
