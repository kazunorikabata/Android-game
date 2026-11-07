package com.kabata.sanpo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabata.sanpo.Explore
import com.kabata.sanpo.meisho.MeishoRepo
import org.osmdroid.views.overlay.Marker
import com.kabata.sanpo.progressBetween
import com.kabata.sanpo.walk.FogOverlay
import com.kabata.sanpo.walk.RouteOverlay
import com.kabata.sanpo.walk.WalkService
import com.kabata.sanpo.walk.WalkTracker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

private fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private val locationPermissions = buildList {
    add(Manifest.permission.ACCESS_FINE_LOCATION)
    add(Manifest.permission.ACCESS_COARSE_LOCATION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val walk by WalkTracker.state.collectAsStateWithLifecycle()
    var hasLocation by remember { mutableStateOf(hasLocationPermission(context)) }
    var pendingStart by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasLocation = hasLocationPermission(context)
        permissionDenied = !hasLocation
        if (hasLocation && pendingStart) WalkService.start(context)
        pendingStart = false
    }

    val fogColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.62f).toArgb()
    val routeColor = MaterialTheme.colorScheme.secondary.toArgb()
    val routeWidth = with(LocalDensity.current) { 5.dp.toPx() }

    val fog = remember { FogOverlay(fogColor) }
    val route = remember { RouteOverlay(routeColor, routeWidth) }
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(17.0)
            controller.setCenter(WalkTracker.state.value.lastPoint ?: GeoPoint(35.6812, 139.7671))
            overlays.add(fog)
            overlays.add(route)
        }
    }
    var myLocation by remember { mutableStateOf<MyLocationNewOverlay?>(null) }
    val meisho by MeishoRepo.state.collectAsStateWithLifecycle()
    val markers = remember { mutableMapOf<String, Marker>() }

    LaunchedEffect(hasLocation) {
        if (hasLocation && myLocation == null) {
            val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                enableMyLocation()
                if (WalkTracker.state.value.lastPoint == null) runOnFirstFix {
                    mapView.post { myLocation?.myLocation?.let { mapView.controller.animateTo(it) } }
                }
            }
            mapView.overlays.add(overlay)
            myLocation = overlay
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    myLocation?.enableMyLocation()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    myLocation?.disableMyLocation()
                    mapView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            myLocation?.disableMyLocation()
            mapView.onDetach()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = {
                fog.cells = walk.cells
                route.segments = walk.segments
                // 名所のピン（訪問ずみは薄く表示）
                val ids = meisho.spots.mapTo(HashSet()) { s -> s.id }
                markers.keys.filter { id -> id !in ids }.forEach { id -> markers.remove(id)?.let { m -> it.overlays.remove(m) } }
                val visited = meisho.visitedIds
                meisho.spots.forEach { s ->
                    val marker = markers.getOrPut(s.id) {
                        Marker(it).apply {
                            position = GeoPoint(s.lat, s.lon)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "${s.emoji} ${s.name}"
                            it.overlays.add(this)
                        }
                    }
                    marker.snippet = "${s.category}・${s.points}pt" + if (s.id in visited) "（訪問ずみ）" else ""
                    marker.alpha = if (s.id in visited) 0.45f else 1f
                }
                it.invalidate()
            },
        )

        StatsCard(walk, permissionDenied, Modifier.align(Alignment.TopCenter).padding(12.dp))

        SmallFloatingActionButton(
            onClick = {
                myLocation?.let { overlay ->
                    overlay.enableFollowLocation()
                    overlay.myLocation?.let { mapView.controller.animateTo(it) }
                } ?: launcher.launch(locationPermissions)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("📍", fontSize = 20.sp) }

        Button(
            onClick = {
                when {
                    walk.recording -> WalkService.stop(context)
                    hasLocation -> {
                        WalkService.start(context)
                        myLocation?.enableFollowLocation()
                    }
                    else -> {
                        pendingStart = true
                        launcher.launch(locationPermissions)
                    }
                }
            },
            colors = if (walk.recording) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            } else ButtonDefaults.buttonColors(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) {
            Text(
                if (walk.recording) "⏹ おさんぽをおわる" else "▶ おさんぽをはじめる",
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun StatsCard(walk: WalkTracker.State, permissionDenied: Boolean, modifier: Modifier) {
    val count = walk.cells.size
    val rankIndex = Explore.rankIndexFor(count)
    val rank = Explore.ranks[rankIndex]
    val next = Explore.ranks.getOrNull(rankIndex + 1)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🧭 ${rank.name}", fontWeight = FontWeight.Bold)
                Text(if (walk.recording) "● 記録中" else "", color = MaterialTheme.colorScheme.secondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("きょう %.2f km".format(walk.todayMeters / 1000))
                Text("ぬりつぶし $count マス")
            }
            if (next != null) {
                val p = progressBetween(count.toLong(), rank.cells.toLong(), next.cells.toLong())
                LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                Text(
                    "あと ${next.cells - count} マスで「${next.name}」",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when {
                permissionDenied -> Text(
                    "位置情報が許可されていません。設定アプリから許可してください。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                count == 0 -> Text(
                    "歩いた場所だけ霧が晴れていきます。乗り物での移動は数えません。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
