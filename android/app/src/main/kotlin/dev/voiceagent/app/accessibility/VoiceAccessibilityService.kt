package dev.voiceagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityButtonController
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import dev.voiceagent.app.allowlist.AllowlistStore
import dev.voiceagent.app.insert.TextInserter
import dev.voiceagent.app.overlay.BubbleOverlay
import dev.voiceagent.app.speech.SpeechActivity
import java.util.UUID

class VoiceAccessibilityService : AccessibilityService() {
    private data class Target(
        val id: String,
        val node: AccessibilityNodeInfo,
        val packageName: String,
        val windowId: Int,
        val text: String,
        val start: Int,
        val end: Int,
        var accepted: Boolean = false,
    )
    private lateinit var allowlist: AllowlistStore
    private var bubble: BubbleOverlay? = null
    val isBubbleShowing: Boolean get() = bubble?.isShowing == true
    val isDictating: Boolean get() = target != null
    private val handler = Handler(Looper.getMainLooper())
    private var target: Target? = null
    private var receiverRegistered = false
    private val shortcut = object : AccessibilityButtonController.AccessibilityButtonCallback() {
        override fun onClicked(controller: AccessibilityButtonController) {
            try { startActivity(Intent(this@VoiceAccessibilityService, dev.voiceagent.app.MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("destination", "home")) }
            catch (_: RuntimeException) { Toast.makeText(this@VoiceAccessibilityService, "Could not open Varnuvi. Open it from your app list.", Toast.LENGTH_LONG).show() }
        }
    }
    private fun shortcutBounds(): android.graphics.Rect? {
        val maxSize = (120 * resources.displayMetrics.density).toInt()
        // SystemUI exposes the shortcut as its own small system window, even
        // when its node tree is unavailable while the shortcut is dormant.
        return windows.firstNotNullOfOrNull { window ->
            val bounds = android.graphics.Rect().also(window::getBoundsInScreen)
            if (window.type == AccessibilityWindowInfo.TYPE_SYSTEM &&
                bounds.width() in 1..maxSize && bounds.height() in 1..maxSize &&
                (window.root?.packageName == null || window.root?.packageName?.toString() == "com.android.systemui")) bounds else null
        }
    }
    private val refresh = Runnable { refreshBubble() }
    private val reconcile = object : Runnable {
        override fun run() {
            refreshBubble()
        }
    }
    private val preferenceChanges = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refreshBubble() }
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                target = null
                bubble?.hide()
                handler.removeCallbacks(refresh)
                handler.removeCallbacks(reconcile)
            } else refreshBubble()
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        accessibilityButtonController.unregisterAccessibilityButtonCallback(shortcut)
        accessibilityButtonController.registerAccessibilityButtonCallback(shortcut, handler)
        if (::allowlist.isInitialized) allowlist.removeListener(preferenceChanges)
        allowlist = AllowlistStore(this)
        allowlist.addListener(preferenceChanges)
        bubble?.hide()
        bubble = BubbleOverlay(this, ::beginDictation)
        if (!receiverRegistered) {
            registerReceiver(screen, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
            }, Context.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        refreshBubble()
    }
    private fun foregroundRoot(): AccessibilityNodeInfo? {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
            !getSystemService(PowerManager::class.java).isInteractive) return null
        val currentWindows = windows
        // Keyboard and our non-focusable overlay must not replace the application.
        val candidates = currentWindows.filter {
            it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD &&
                it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
        }
        val current = candidates.firstOrNull { it.isActive } ?: candidates.firstOrNull { it.isFocused }
        if (current != null) {
            if (current.type == AccessibilityWindowInfo.TYPE_APPLICATION) return current.root
            // The floating system shortcut may stay active after being touched.
            // Only that small window may defer to the focused application.
            val bounds = android.graphics.Rect().also(current::getBoundsInScreen)
            val limit = (120 * resources.displayMetrics.density).toInt()
            if (current.type == AccessibilityWindowInfo.TYPE_SYSTEM && bounds.width() <= limit && bounds.height() <= limit) {
                return candidates.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.isFocused }?.root
            }
            return null
        }
        return if (currentWindows.isEmpty()) rootInActiveWindow else null
    }
    private fun eligibleField(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val pkg = root.packageName?.toString() ?: return null
        if (pkg == packageName || !allowlist.isEnabled(pkg)) return null
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        return node.takeIf { it.packageName?.toString() == pkg && it.windowId == root.windowId && FocusedField.isEligible(it) }
    }
    private fun scheduleReconcile() {
        handler.removeCallbacks(reconcile)
        val delay = BubbleRefreshPolicy.delay(
            interactive = getSystemService(PowerManager::class.java).isInteractive,
            hasApps = ::allowlist.isInitialized && allowlist.enabledPackages().isNotEmpty(),
            dictating = target != null,
            visible = isBubbleShowing,
        )
        if (delay != null) handler.postDelayed(reconcile, delay)
    }
    private fun refreshBubble() {
        if (!::allowlist.isInitialized || target != null || allowlist.enabledPackages().isEmpty() ||
            !getSystemService(PowerManager::class.java).isInteractive) {
            bubble?.hide(); scheduleReconcile(); return
        }
        try {
            val root = foregroundRoot()
            val pkg = root?.packageName?.toString()
            if (pkg != null && pkg != packageName && allowlist.isEnabled(pkg)) {
                bubble?.show(shortcutBounds())
            } else bubble?.hide()
        } catch (_: RuntimeException) { bubble?.hide() }
        finally { scheduleReconcile() }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::allowlist.isInitialized) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            refreshBubble()
            if (!handler.hasCallbacks(refresh)) handler.postDelayed(refresh, 100)
        } else if (!handler.hasCallbacks(refresh)) handler.postDelayed(refresh, 200)
    }

    private fun nodeText(node: AccessibilityNodeInfo) = if (node.isShowingHintText) "" else node.text?.toString().orEmpty()
    private fun beginDictation() {
        if (target != null) return
        try {
            val root = foregroundRoot() ?: return refreshBubble()
            val node = eligibleField(root) ?: run {
                Toast.makeText(this, "Tap a text field first, then tap the microphone.", Toast.LENGTH_SHORT).show()
                return refreshBubble()
            }
            val next = Target(UUID.randomUUID().toString(), node, node.packageName.toString(),
                node.windowId, nodeText(node), node.textSelectionStart, node.textSelectionEnd)
            target = next
            bubble?.hide()
            startActivity(Intent(this, SpeechActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("targetPackage", next.packageName)
                putExtra("targetSession", next.id)
            })
        } catch (_: RuntimeException) {
            target = null
            Toast.makeText(this, "Could not open dictation. Try again.", Toast.LENGTH_LONG).show()
            refreshBubble()
        }
    }
    fun cancelDictation(session: String?) {
        if (session != null && target?.id == session) { target = null; refreshBubble() }
    }
    /** Session identity prevents stale, duplicate, or cancelled results from inserting. */
    fun deliver(text: String, pkg: String, saved: Boolean, session: String?): Boolean {
        val original = target ?: return false
        if (session != original.id || pkg != original.packageName || original.accepted || text.isBlank()) return false
        original.accepted = true
        var attempts = 0
        fun finish(failed: Boolean) {
            if (target?.id == original.id) target = null
            if (failed) notifyFallback(saved)
            refreshBubble()
        }
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (target?.id != original.id) { notifyFallback(saved); return }
                try {
                    if (!allowlist.isEnabled(pkg)) { finish(true); return }
                    val root = foregroundRoot()
                    if (root?.packageName?.toString() == pkg) {
                        if (root.windowId != original.windowId) { finish(true); return }
                        val node = original.node
                        if (!node.refresh() || node.windowId != original.windowId ||
                            !FocusedField.isEligible(node) || nodeText(node) != original.text) { finish(true); return }
                        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                        if (focused != null && focused != node) { finish(true); return }
                        if (focused == null) {
                            if (!node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) { finish(true); return }
                        } else {
                            // Restore the original selection only when its content is unchanged.
                            if (original.start >= 0 && original.end >= 0 &&
                                (node.textSelectionStart != original.start || node.textSelectionEnd != original.end)) {
                                val restored = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, minOf(original.start, original.end))
                                    putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, maxOf(original.start, original.end))
                                })
                                if (!restored) { finish(true); return }
                            }
                            val result = TextInserter.insert(this@VoiceAccessibilityService, node, text)
                            if (result.path == TextInserter.Path.FAILED) { finish(true); return }
                            // Some apps acknowledge an accessibility action without applying it.
                            // Check once; never paste again after an acknowledged insertion.
                            handler.postDelayed({
                                val verified = try { node.refresh() && nodeText(node) == result.expectedText } catch (_: RuntimeException) { false }
                                finish(!verified)
                            }, 250)
                            return
                        }
                    } else if (root != null && root.packageName?.toString() != packageName) {
                        finish(true); return
                    }
                    if (++attempts < 15) handler.postDelayed(this, 200) else finish(true)
                } catch (_: RuntimeException) { finish(true) }
            }
        }, 300)
        return true
    }
    private fun notifyFallback(saved: Boolean) {
        Toast.makeText(this, if (saved) "Could not insert here. Your transcript is in Varnuvi History." else "Could not insert or save. Please try again.", Toast.LENGTH_LONG).show()
    }
    override fun onInterrupt() { target = null; bubble?.hide(); scheduleReconcile() }
    override fun onUnbind(intent: Intent?): Boolean { handler.removeCallbacksAndMessages(null); target = null; bubble?.hide(); instance = null; return super.onUnbind(intent) }
    override fun onDestroy() {
        accessibilityButtonController.unregisterAccessibilityButtonCallback(shortcut)
        handler.removeCallbacksAndMessages(null)
        target = null
        bubble?.hide(); bubble = null
        if (::allowlist.isInitialized) allowlist.removeListener(preferenceChanges)
        if (receiverRegistered) { unregisterReceiver(screen); receiverRegistered = false }
        instance = null
        super.onDestroy()
    }
    companion object { var instance: VoiceAccessibilityService? = null; private set }
}
