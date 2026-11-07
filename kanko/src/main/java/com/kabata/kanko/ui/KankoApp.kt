package com.kabata.kanko.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabata.kanko.data.Visit
import com.kabata.kanko.data.VisitRepo
import com.kabata.kanko.data.rememberCurrentLocation
import java.io.File

private fun hasFineLocation(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private val locationPermissions =
    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

@Composable
fun KankoApp() {
    val context = LocalContext.current
    val st by VisitRepo.state.collectAsStateWithLifecycle()

    var hasLocation by remember { mutableStateOf(hasFineLocation(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { hasLocation = hasFineLocation(context) }
    LaunchedEffect(Unit) { if (!hasLocation) permissionLauncher.launch(locationPermissions) }
    val location = rememberCurrentLocation(hasLocation)

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var celebrate by remember { mutableStateOf<Visit?>(null) }

    var pendingId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val id = pendingId
        val file = pendingPath?.let(::File)
        if (ok && id != null && file != null && file.exists() && file.length() > 0) {
            celebrate = VisitRepo.addVisit(id, file, onSite = true)
            selected = null
        } else {
            file?.delete()
        }
        pendingId = null
        pendingPath = null
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = pendingId
        pendingId = null
        if (uri != null && id != null) {
            val file = VisitRepo.newPhotoFile(context)
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }.isSuccess
            if (copied) {
                celebrate = VisitRepo.addVisit(id, file, onSite = false)
                selected = null
            } else {
                file.delete()
                Toast.makeText(context, "写真を読みこめませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun takePhoto(id: String) {
        val file = VisitRepo.newPhotoFile(context)
        pendingId = id
        pendingPath = file.absolutePath
        try {
            camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "カメラアプリが見つかりません", Toast.LENGTH_SHORT).show()
        }
    }

    fun pickPast(id: String) {
        pendingId = id
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                listOf("🏠" to "ホーム", "🗾" to "ちず", "📍" to "スポット", "📷" to "アルバム")
                    .forEachIndexed { i, (icon, label) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Text(icon, fontSize = 20.sp) },
                            label = { Text(label) },
                        )
                    }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            val open: (String) -> Unit = { selected = it }
            when (tab) {
                0 -> HomeScreen(st, location, onOpen = open)
                1 -> SpotMapScreen(st, hasLocation, location, onOpen = open)
                2 -> ListScreen(st, location, onOpen = open)
                else -> AlbumScreen(st)
            }
        }
    }

    selected?.let { id ->
        SpotDialog(
            id = id,
            st = st,
            location = location,
            hasLocation = hasLocation,
            onDismiss = { selected = null },
            onCheckIn = { takePhoto(id) },
            onPastPhoto = { pickPast(id) },
            onRequestPermission = { permissionLauncher.launch(locationPermissions) },
        )
    }

    celebrate?.let { v -> CelebrateDialog(v, st, onDismiss = { celebrate = null }) }
}
