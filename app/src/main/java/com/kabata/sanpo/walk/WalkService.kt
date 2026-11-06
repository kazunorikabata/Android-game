package com.kabata.sanpo.walk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.kabata.sanpo.MainActivity
import com.kabata.sanpo.R

/** 画面を消していても GPS でお散歩を記録するフォアグラウンドサービス */
class WalkService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private var listening = false

    override fun onCreate() {
        super.onCreate()
        WalkTracker.init(applicationContext)
        locationManager = getSystemService(LocationManager::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!granted) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!listening) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 2_000L, 3f, this, Looper.getMainLooper(),
                )
                listening = true
                WalkTracker.startSession()
            } catch (e: Exception) {
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onLocationChanged(location: Location) {
        WalkTracker.addFix(location)
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
    }

    // Android 10 以前ではこれらが必須メソッドなので明示的に実装しておく
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onDestroy() {
        if (listening) locationManager.removeUpdates(this)
        listening = false
        WalkTracker.stopSession()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): android.app.Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm?.getNotificationChannel(CHANNEL_ID) == null) {
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "おさんぽ記録", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, WalkService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val s = WalkTracker.state.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("おさんぽを記録中 🐾")
            .setContentText("きょう %.2f km ・ ぬりつぶし %d マス".format(s.todayMeters / 1000, s.cells.size))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "おわる", stop)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "walk"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.kabata.sanpo.STOP_WALK"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WalkService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WalkService::class.java))
        }
    }
}
