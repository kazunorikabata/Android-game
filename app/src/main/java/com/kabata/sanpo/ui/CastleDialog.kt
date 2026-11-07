package com.kabata.sanpo.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.kabata.sanpo.data.CastleRepo
import com.kabata.sanpo.data.Castles
import com.kabata.sanpo.data.Visit
import java.io.File

@Composable
fun CastleDialog(
    no: Int,
    st: CastleRepo.State,
    location: Location?,
    hasLocation: Boolean,
    onDismiss: () -> Unit,
    onCheckIn: () -> Unit,
    onPastPhoto: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current
    val castle = Castles.byNo(no)
    val visits = st.visitsFor(no).sortedByDescending { it.time }
    val first = st.firstVisit(no)
    val distance = location?.let { CastleRepo.distanceTo(castle, it) }
    val near = CastleRepo.isNear(castle, location)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("とじる") } },
        dismissButton = {
            TextButton(onClick = {
                val uri = Uri.parse("geo:0,0?q=${castle.lat},${castle.lon}(${Uri.encode(castle.name)})")
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: ActivityNotFoundException) {
                }
            }) { Text("🧭 道案内") }
        },
        title = {
            Column {
                Text("No.${castle.no}・${castle.pref}", style = MaterialTheme.typography.labelLarge)
                Text(castle.name, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (first != null) "✅ 登城ずみ（${formatDate(first.time)}）" else "まだ登城していません",
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
                                model = File(v.photo), contentDescription = castle.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }

                if (near) {
                    Button(onClick = onCheckIn, modifier = Modifier.fillMaxWidth()) {
                        Text("📷 写真をとって登城！")
                    }
                } else if (distance != null) {
                    Text(
                        "お城の近く（約 ${formatDistance(castle.radius.toFloat())} 以内）に行くと、写真をとって登城できます。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(onClick = onPastPhoto, modifier = Modifier.fillMaxWidth()) {
                    Text("🖼 前に行ったときの写真で記録")
                }
            }
        },
    )
}

@Composable
fun CelebrateDialog(visit: Visit, st: CastleRepo.State, onDismiss: () -> Unit) {
    val castle = Castles.byNo(visit.no)
    val isFirst = st.visitsFor(visit.no).size == 1
    val count = st.count
    val rankNow = Castles.rankIndexFor(count)
    val rankUp = isFirst && rankNow > Castles.rankIndexFor(count - 1)
    val region = Castles.regionOf(visit.no)
    val regionDone = isFirst && region.range.all { it in st.visitedNos }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("やったー！") } },
        title = { Text(if (isFirst) "🏯 登城！" else "📷 写真を追加しました") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = File(visit.photo), contentDescription = castle.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.height(10.dp))
                Text("No.${castle.no} ${castle.name}", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (isFirst) {
                    Text(
                        "$count / 100 城目",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (rankUp) {
                    Text(
                        "称号が「${Castles.ranks[rankNow].name}」になりました！",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (regionDone) {
                    Text(
                        "🎉 ${region.name}のお城をすべて制覇！",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (!visit.onSite) {
                    Text("（過去の写真から登録）", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}
