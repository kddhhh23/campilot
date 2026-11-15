package com.example.campilot.navigation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.campilot.MainActivity

/** Keeps the explicitly started Pleos navigation callback session out of cached/frozen state. */
class NavigationSessionService : Service() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "차박지 이동 안내", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(4101, Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("Campilot · 이동 중")
            .setContentText("내비 안내 종료 알림을 기다리고 있어요.")
            .setContentIntent(open).setOngoing(true).build())
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }
    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "campilot_navigation_session"
        fun start(context: Context) {
            context.startForegroundService(Intent(context, NavigationSessionService::class.java))
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, NavigationSessionService::class.java))
        }
    }
}
