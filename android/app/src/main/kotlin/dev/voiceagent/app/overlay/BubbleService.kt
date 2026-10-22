package dev.voiceagent.app.overlay

import android.app.*
import android.content.*
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import dev.voiceagent.app.MainActivity
import dev.voiceagent.app.R
import dev.voiceagent.app.speech.SpeechActivity

/** User-started overlay only. Never inspects other apps or records audio in a service. */
class BubbleService : Service() {
    private var bubble: BubbleOverlay? = null
    var dictating = false
        private set
    val visible: Boolean get() = bubble?.isShowing == true
    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) bubble?.hide()
            else showBubble()
        }
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        bubble = BubbleOverlay(this, ::record)
        registerReceiver(screen, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }, RECEIVER_NOT_EXPORTED)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == HIDE || !Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Floating bubble", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java)
            .putExtra("destination", "bubble"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val hide = PendingIntent.getService(this, 2, Intent(this, BubbleService::class.java).setAction(HIDE), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bubble_notification)
            .setContentTitle("Your dictation bubble is on")
            .setContentText("Tap the bubble to speak. Copy your words, then paste.")
            .setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Hide bubble", hide).build())
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(41, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(41, notification)
        showBubble()
        return START_NOT_STICKY
    }
    private fun record() {
        if (dictating) return
        val bounds = bubble?.boundsOnScreen()
        // Keep the visible overlay until startActivity: it authorizes this user-initiated launch.
        try {
            startActivity(Intent(this, SpeechActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("overlayDictation", true)
                bounds?.let { putExtra("micCenterX", it.centerX()); putExtra("micCenterY", it.centerY()) }
            })
            dictating = true
            bubble?.hide()
        } catch (_: RuntimeException) {
            Toast.makeText(this, "Could not open dictation. Open Varnuvi and try again.", Toast.LENGTH_LONG).show()
        }
    }
    fun pauseBubble() { dictating = true; bubble?.hide() }
    fun resumeBubble() { dictating = false; showBubble() }
    private fun showBubble() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        if (!dictating && getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked) bubble?.show()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        bubble?.hide()
        unregisterReceiver(screen)
        instance = null
        super.onDestroy()
    }
    companion object {
        var instance: BubbleService? = null
            private set
        private const val CHANNEL = "dictation_bubble"
        private const val HIDE = "dev.voiceagent.app.HIDE_BUBBLE"
    }
}
