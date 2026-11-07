package com.kabata.sanpo.meisho

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** 画面を表示している間だけ現在地を受け取る（GPS とネットワーク位置の両方） */
@SuppressLint("MissingPermission")
@Composable
fun rememberCurrentLocation(enabled: Boolean): Location? {
    val context = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }

    DisposableEffect(enabled) {
        val lm = context.getSystemService(LocationManager::class.java)
        if (!enabled || lm == null) return@DisposableEffect onDispose { }

        fun offer(l: Location) {
            val cur = location
            // 新しい測位が明らかに悪い精度でなければ採用。古い位置（20秒以上前）は更新する
            if (cur == null || l.time - cur.time > 20_000 || l.accuracy <= cur.accuracy + 15) location = l
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) = offer(l)
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (!lm.isProviderEnabled(p)) continue
                lm.getLastKnownLocation(p)?.let(::offer)
                lm.requestLocationUpdates(p, 3_000L, 3f, listener, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
        } catch (e: IllegalArgumentException) {
        }
        onDispose { lm.removeUpdates(listener) }
    }
    return location
}
