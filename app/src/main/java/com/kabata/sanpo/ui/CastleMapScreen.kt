package com.kabata.sanpo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kabata.sanpo.R
import com.kabata.sanpo.data.CastleRepo
import com.kabata.sanpo.data.Castles
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun CastleMapScreen(st: CastleRepo.State, hasLocation: Boolean, onOpen: (Int) -> Unit) {
    val context = LocalContext.current
    val currentOnOpen by rememberUpdatedState(onOpen)
    val visitedColor = MaterialTheme.colorScheme.primary.toArgb()
    val todoColor = android.graphics.Color.rgb(120, 120, 120)

    val pinVisited = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_pin)!!.mutate().apply { setTint(visitedColor) }
    }
    val pinTodo = remember {
        ContextCompat.getDrawable(context, R.drawable.ic_pin)!!.mutate().apply { setTint(todoColor) }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            controller.setZoom(5.5)
            controller.setCenter(GeoPoint(36.2, 138.25))
        }
    }
    val markers = remember {
        Castles.all.associate { castle ->
            castle.no to Marker(mapView).apply {
                position = GeoPoint(castle.lat, castle.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "No.${castle.no} ${castle.name}"
                setOnMarkerClickListener { _, _ ->
                    currentOnOpen(castle.no)
                    true
                }
                mapView.overlays.add(this)
            }
        }
    }
    var myLocation by remember { mutableStateOf<MyLocationNewOverlay?>(null) }

    LaunchedEffect(hasLocation) {
        if (hasLocation && myLocation == null) {
            val overlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply { enableMyLocation() }
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
                markers.forEach { (no, marker) ->
                    marker.icon = if (no in st.visitedNos) pinVisited else pinTodo
                }
                it.invalidate()
            },
        )

        Card(
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        ) {
            Text(
                "🏯 登城 ${st.count} / 100　（赤＝登城ずみ）",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
            )
        }

        SmallFloatingActionButton(
            onClick = {
                myLocation?.myLocation?.let {
                    mapView.controller.setZoom(12.0)
                    mapView.controller.animateTo(it)
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Text("📍", fontSize = 20.sp) }

        SmallFloatingActionButton(
            onClick = {
                mapView.controller.setZoom(5.5)
                mapView.controller.animateTo(GeoPoint(36.2, 138.25))
            },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
        ) { Text("🗾", fontSize = 20.sp) }
    }
}
