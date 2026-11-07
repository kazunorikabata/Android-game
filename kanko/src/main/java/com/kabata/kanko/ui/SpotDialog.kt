package com.kabata.kanko.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kabata.kanko.data.Spots
import com.kabata.kanko.data.Visit
import com.kabata.kanko.data.VisitRepo
import java.io.File

@Composable
fun SpotDialog(
    id: String,
    st: VisitRepo.State,
    location: Location?,
    hasLocation: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: () -> Unit,
    onPastPhoto: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current
    val spot = Spots.byId(id) ?: return
    val visits = st.visitsFor(id).sortedByDescending { it.time }
    val first = st.firstVisit(id)
    val distance = location?.let { Spots.distanceTo(spot, it) }
    val near = Spots.isNear(spot, location)

    fun open(uri: Uri) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("とじる") } },
        title = {
            Column {
                Text(spot.pref, style = MaterialTheme.typography.labelLarge)
                Text("${spot.emoji} ${spot.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(spot.desc)
                Text(
                    if (first != null) "✅ 訪問ずみ（${formatDate(first.time)}）" else "まだ訪れていません",
                    fontWeight = FontWeight.Bold,
                    color = if (first != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                when {
                    !hasLocation -> TextButton(onClick = onRequestPermission) { Text("📍 位置情報を許可して距離を表示") }
                    distance == null -> Text("現在地を確認中…", style = MaterialTheme.typography.bodySmall)
                    else -> Text("現在地から ${formatDistance(distance)}", style = MaterialTheme.typography.bodySmall)
                }
                if (visits.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visits, key = { it.photo }) { v ->
                            AsyncImage(
                                model = File(v.photo), contentDescription = spot.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }
                if (near) {
                    Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                        Text("📷 写真をとってチェックイン！")
                    }
                } else if (distance != null) {
                    Text(
                        "近く（約 ${formatDistance(spot.radius.toFloat())} 以内）に行くと、写真をとってチェックインできます。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onPastPhoto, modifier = Modifier.fillMaxWidth()) {
                    Text("🖼 前に行ったときの写真で記録")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { open(Uri.parse("geo:0,0?q=${spot.lat},${spot.lon}(${Uri.encode(spot.name)})")) },
                        modifier = Modifier.weight(1f),
                    ) { Text("🧭 道案内") }
                    OutlinedButton(
                        onClick = {
                            open(Uri.parse("https://www.google.com/search?q=" + Uri.encode("${spot.name} ${spot.pref}")))
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("🔎 調べる") }
                }
            }
        },
    )
}

@Composable
fun CelebrateDialog(visit: Visit, st: VisitRepo.State, onDismiss: () -> Unit) {
    val spot = Spots.byId(visit.spotId) ?: return
    val isFirst = st.visitsFor(visit.spotId).size == 1
    val count = st.count
    val rankNow = Spots.rankIndexFor(count)
    val rankUp = isFirst && rankNow > Spots.rankIndexFor(count - 1)
    val prefTotal = Spots.inPref(spot.pref).size
    val prefDone = st.countIn(spot.pref)
    val firstInPref = isFirst && prefDone == 1

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("やったー！") } },
        title = { Text(if (isFirst) "📍 チェックイン！" else "📷 写真を追加しました") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = File(visit.photo), contentDescription = spot.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text("${spot.emoji} ${spot.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
                if (isFirst) {
                    Text(
                        "${spot.pref} $prefDone / $prefTotal か所",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (firstInPref) {
                    Text("${spot.pref}デビュー！（${st.visitedPrefs.size} / 47 都道府県）", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
                if (isFirst && prefDone == prefTotal) {
                    Text("🎉 ${spot.pref}のスポットをすべて制覇！", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
                if (rankUp) {
                    Text("称号が「${Spots.ranks[rankNow].name}」になりました！", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
                if (!visit.onSite) Text("（過去の写真から登録）", style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}
