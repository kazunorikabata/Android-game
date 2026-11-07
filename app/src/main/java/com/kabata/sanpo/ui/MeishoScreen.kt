package com.kabata.sanpo.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kabata.sanpo.meisho.Meisho
import com.kabata.sanpo.meisho.MeishoRepo
import com.kabata.sanpo.meisho.Visit
import com.kabata.sanpo.meisho.rememberCurrentLocation
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun hasFineLocation(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private val dateFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
private fun formatTime(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).format(dateFormat)

private fun formatDistance(m: Float) = if (m < 1000) "${m.toInt()} m" else "%.1f km".format(m / 1000)

@Composable
fun MeishoScreen() {
    val context = LocalContext.current
    val st by MeishoRepo.state.collectAsStateWithLifecycle()
    var hasLocation by remember { mutableStateOf(hasFineLocation(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { hasLocation = hasFineLocation(context) }
    val location = rememberCurrentLocation(hasLocation)
    val scope = rememberCoroutineScope()

    var page by rememberSaveable { mutableIntStateOf(0) }
    var pendingSpotId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var justVisited by remember { mutableStateOf<Visit?>(null) }
    var viewing by remember { mutableStateOf<Visit?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val spot = pendingSpotId?.let(MeishoRepo::spot)
        val file = pendingPhoto?.let(::File)
        if (ok && spot != null && file != null && file.exists() && file.length() > 0) {
            justVisited = MeishoRepo.checkIn(spot, file)
        } else {
            file?.delete()
        }
        pendingSpotId = null
        pendingPhoto = null
    }

    fun takePhoto(spot: Meisho) {
        val file = File(MeishoRepo.photoDir(context), "${spot.id}_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingSpotId = spot.id
        pendingPhoto = file.absolutePath
        try {
            camera.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "カメラアプリが見つかりません", Toast.LENGTH_SHORT).show()
        }
    }

    // 現在地がわかったら、まだ名所を探していなければ自動で探す
    LaunchedEffect(location != null) {
        val loc = location
        if (loc != null && MeishoRepo.state.value.spots.isEmpty() && !MeishoRepo.state.value.loading) {
            MeishoRepo.search(loc.latitude, loc.longitude)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "めいしょめぐり",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("⭐ ${st.totalPoints} pt", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.size(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleButton("📍 ちかくの名所", page == 0) { page = 0 }
            ToggleButton("📷 アルバム（${st.visits.size}）", page == 1) { page = 1 }
        }
        Spacer(Modifier.size(8.dp))

        if (page == 0) {
            NearbyPage(
                st = st,
                hasLocation = hasLocation,
                location = location,
                onRequestPermission = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                },
                onSearch = { loc -> scope.launch { MeishoRepo.search(loc.latitude, loc.longitude) } },
                onTakePhoto = ::takePhoto,
            )
        } else {
            AlbumPage(st.visits, onOpen = { viewing = it })
        }
    }

    justVisited?.let { v ->
        AlertDialog(
            onDismissRequest = { justVisited = null },
            confirmButton = { TextButton(onClick = { justVisited = null }) { Text("やったー！") } },
            title = { Text(if (v.points > 0) "チェックイン！" else "アルバムに追加しました") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = File(v.photo), contentDescription = v.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("${v.emoji} ${v.name}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(
                        if (v.points > 0) "+${v.points} ポイント！" else "ポイントは1か所につき1日1回です",
                        fontSize = if (v.points > 0) 24.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            },
        )
    }

    viewing?.let { v ->
        AlertDialog(
            onDismissRequest = { viewing = null },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("とじる") } },
            title = { Text("${v.emoji} ${v.name}") },
            text = {
                Column {
                    AsyncImage(
                        model = File(v.photo), contentDescription = v.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(formatTime(v.time))
                    Text(if (v.points > 0) "+${v.points} pt" else "ポイントなし")
                }
            },
        )
    }
}

@Composable
private fun ToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else OutlinedButton(onClick = onClick) { Text(label) }
}

@Composable
private fun NearbyPage(
    st: MeishoRepo.State,
    hasLocation: Boolean,
    location: android.location.Location?,
    onRequestPermission: () -> Unit,
    onSearch: (android.location.Location) -> Unit,
    onTakePhoto: (Meisho) -> Unit,
) {
    if (!hasLocation) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("まわりの名所を探すには、位置情報の許可が必要です。")
                Button(onClick = onRequestPermission) { Text("許可する") }
            }
        }
        return
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            when {
                location == null -> "現在地を確認中…"
                else -> "現在地から ${MeishoRepo.SEARCH_RADIUS_M / 1000.0} km 以内（${st.spots.size} か所）"
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (st.loading) {
            CircularProgressIndicator(Modifier.size(24.dp))
        } else {
            TextButton(onClick = { location?.let(onSearch) }, enabled = location != null) { Text("🔄 さがす") }
        }
    }
    st.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

    if (!st.loading && st.spots.isEmpty() && location != null && st.error == null) {
        Text(
            "近くに名所が見つかりませんでした。少し歩いてから「さがす」を押してみてください。",
            modifier = Modifier.padding(vertical = 16.dp),
        )
    }

    val visited = st.visitedIds
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(st.spots, key = { it.id }) { spot ->
            val distance = location?.let { MeishoRepo.distanceTo(spot, it) }
            // GPS の誤差も少しだけ考慮する（最大 50m）
            val slack = location?.accuracy?.coerceAtMost(50f) ?: 0f
            val near = distance != null && distance <= spot.radius + slack
            val points = MeishoRepo.pointsFor(spot)
            SpotCard(spot, distance, near, spot.id in visited, points, onTakePhoto = { onTakePhoto(spot) })
        }
    }
}

@Composable
private fun SpotCard(
    spot: Meisho,
    distance: Float?,
    near: Boolean,
    visited: Boolean,
    points: Int,
    onTakePhoto: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (near) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(spot.emoji, fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(spot.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        spot.category + if (visited) "・✅ 訪問ずみ" else "",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        when {
                            points == 0 -> "撮影ずみ"
                            visited -> "+$points pt"
                            else -> "+$points pt"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (distance != null) Text(formatDistance(distance), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (near) {
                Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Text("📷 写真をとってチェックイン")
                }
            } else if (distance != null) {
                Text(
                    "あと ${formatDistance(distance - spot.radius)} 近づくと写真がとれます",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun AlbumPage(visits: List<Visit>, onOpen: (Visit) -> Unit) {
    if (visits.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "まだ写真がありません。\n名所の近くで写真をとると、ここに集まります。",
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(visits.asReversed(), key = { it.photo }) { v ->
            Card(Modifier.clickable { onOpen(v) }) {
                AsyncImage(
                    model = File(v.photo), contentDescription = v.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Column(Modifier.padding(8.dp)) {
                    Text("${v.emoji} ${v.name}", fontWeight = FontWeight.Bold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatTime(v.time) + if (v.points > 0) "  +${v.points}pt" else "",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
