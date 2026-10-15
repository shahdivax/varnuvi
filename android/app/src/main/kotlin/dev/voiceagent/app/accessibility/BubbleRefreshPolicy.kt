package dev.voiceagent.app.accessibility

/** Events provide prompt updates; this is only a recovery and anchor-following check. */
object BubbleRefreshPolicy {
    fun delay(interactive: Boolean, hasApps: Boolean, dictating: Boolean, visible: Boolean): Long? =
        if (!interactive || !hasApps || dictating) null else if (visible) 500L else 5000L
}
