package com.kabata.kanko.ui

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kabata.kanko.data.Spots
import com.kabata.kanko.data.VisitRepo
import java.io.File

@Composable
fun HomeScreen(st: VisitRepo.State, location: Location?, onOpen: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("にっぽん観光めぐり", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        ProgressCard(st)
        location?.let { NearbyCard(st, it, onOpen) }
        RecentCard(st, onOpen)
    }
}

@Composable
private fun ProgressCard(st: VisitRepo.State) {
    val total = Spots.all.size
    val rankIndex = Spots.rankIndexFor(st.count)
    val next = Spots.ranks.getOrNull(rankIndex + 1)
    val conquered = Spots.prefectures.count { pref -> st.countIn(pref) == Spots.inPref(pref).size }
    SectionCard("旅のきろく") {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${st.count}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(" / $total か所", modifier = Modifier.padding(bottom = 8.dp))
        }
        LinearProgressIndicator(progress = { st.count.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        Text(
            "称号：${Spots.ranks[rankIndex].name}",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (next != null) Text("あと ${next.count - st.count} か所で「${next.name}」", style = MaterialTheme.typography.bodySmall)
        Text("訪れた都道府県 ${st.visitedPrefs.size} / 47　・　制覇 $conquered 都道府県")
    }
}

@Composable
private fun NearbyCard(st: VisitRepo.State, location: Location, onOpen: (String) -> Unit) {
    val nearby = Spots.all.map { it to Spots.distanceTo(it, location) }.sortedBy { it.second }.take(5)
    SectionCard("近くのスポット") {
        nearby.forEach { (spot, d) ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(spot.id) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(spot.emoji, fontSize = 26.sp)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(spot.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${spot.pref}・${formatDistance(d)}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    when {
                        spot.id in st.visitedIds -> "✅"
                        Spots.isNear(spot, location) -> "📷"
                        else -> ""
                    },
                    fontSize = 20.sp,
                )
            }
        }
    }
}

@Composable
private fun RecentCard(st: VisitRepo.State, onOpen: (String) -> Unit) {
    SectionCard("さいきんの思い出") {
        if (st.visits.isEmpty()) {
            Text(
                "まだ記録がありません。\n「スポット」や「ちず」から場所を選んで、現地で写真をとってチェックインしましょう。",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@SectionCard
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(st.visits.sortedByDescending { it.time }.take(10), key = { it.photo }) { v ->
                Column(Modifier.width(110.dp).clickable { onOpen(v.spotId) }) {
                    AsyncImage(
                        model = File(v.photo), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(110.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Text(
                        Spots.byId(v.spotId)?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
