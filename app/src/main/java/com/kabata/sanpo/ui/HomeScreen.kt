package com.kabata.sanpo.ui

import android.location.Location
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kabata.sanpo.data.CastleRepo
import com.kabata.sanpo.data.Castles
import java.io.File

@Composable
fun HomeScreen(st: CastleRepo.State, location: Location?, onOpen: (Int) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("百名城めぐり", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        ProgressCard(st)
        location?.let { NearestCard(st, it, onOpen) }
        RecentCard(st, onOpen)
        QuizCard()
        RegionCard(st)
    }
}

@Composable
private fun QuizCard() {
    val best by com.kabata.sanpo.quiz.QuizRepo.best.collectAsStateWithLifecycle()
    val perfect = best.values.count { it.perfect }
    SectionCard("お城クイズ") {
        Text("🎓 満点 $perfect / 100 城", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary)
        Text(
            "挑戦したお城 ${best.size} 城。お城を選んで「❓ クイズに挑戦」から遊べます（登城していなくてもOK）。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ProgressCard(st: CastleRepo.State) {
    val count = st.count
    val rankIndex = Castles.rankIndexFor(count)
    val rank = Castles.ranks[rankIndex]
    val next = Castles.ranks.getOrNull(rankIndex + 1)
    val progress by animateFloatAsState(count / 100f, tween(800), label = "progress")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bar = MaterialTheme.colorScheme.primary

    SectionCard("登城のきろく") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, -90f, 360f, false, topLeft, arc, style = Stroke(stroke))
                    drawArc(bar, -90f, 360f * progress, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                    Text("/ 100 城")
                }
            }
        }
        Text(
            "称号：${rank.name}",
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (next != null) {
            Text("あと ${next.count - count} 城で「${next.name}」", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("全100城を制覇しました！天下統一！", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NearestCard(st: CastleRepo.State, location: Location, onOpen: (Int) -> Unit) {
    val nearest = Castles.all
        .filter { it.no !in st.visitedNos }
        .minByOrNull { CastleRepo.distanceTo(it, location) } ?: return
    val near = CastleRepo.isNear(nearest, location)
    SectionCard("いちばん近い未登城のお城", Modifier.clickable { onOpen(nearest.no) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏯", fontSize = 36.sp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("No.${nearest.no} ${nearest.name}", fontWeight = FontWeight.Bold)
                Text(
                    "${nearest.pref}・${formatDistance(CastleRepo.distanceTo(nearest, location))}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (near) Text("登城できます！", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecentCard(st: CastleRepo.State, onOpen: (Int) -> Unit) {
    if (st.visits.isEmpty()) {
        SectionCard("さいきんの登城") {
            Text(
                "まだ登城記録がありません。\n「いちらん」や「ちず」からお城を選んで、写真で登城しましょう。" +
                    "前に行ったお城は、そのときの写真で記録できます。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }
    SectionCard("さいきんの登城") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(st.visits.sortedByDescending { it.time }.take(10), key = { it.photo }) { v ->
                Column(Modifier.width(110.dp).clickable { onOpen(v.no) }) {
                    AsyncImage(
                        model = File(v.photo), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Text(Castles.byNo(v.no).name, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RegionCard(st: CastleRepo.State) {
    SectionCard("地方べつ") {
        Castles.regions.forEach { region ->
            val total = region.range.count()
            val done = region.range.count { it in st.visitedNos }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        region.name + if (done == total) " 👑" else "",
                        modifier = Modifier.weight(1f),
                    )
                    Text("$done / $total", fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
