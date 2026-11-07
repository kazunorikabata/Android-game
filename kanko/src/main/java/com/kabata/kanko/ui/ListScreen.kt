package com.kabata.kanko.ui

import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabata.kanko.data.Spot
import com.kabata.kanko.data.Spots
import com.kabata.kanko.data.VisitRepo

private const val NEARBY = "📍 近く"

@Composable
fun ListScreen(st: VisitRepo.State, location: Location?, onOpen: (String) -> Unit) {
    // 選択中の都道府県（最初は現在地の都道府県、わからなければ愛知県）
    var pref by rememberSaveable { mutableStateOf<String?>(null) }
    var region by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(location != null) {
        if (pref == null) {
            val p = location?.let { Spots.nearestPref(it) } ?: "愛知県"
            pref = p
            region = Spots.regionOf(p).name
        }
    }
    val currentPref = pref ?: "愛知県"
    val currentRegion = region ?: Spots.regionOf(currentPref).name

    val spots: List<Spot> = if (currentPref == NEARBY && location != null) {
        Spots.all.sortedBy { Spots.distanceTo(it, location) }.take(30)
    } else {
        Spots.inPref(currentPref).let { list ->
            if (location != null) list.sortedBy { Spots.distanceTo(it, location) } else list
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (currentPref == NEARBY) "近くのスポット" else currentPref,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (currentPref != NEARBY) {
                Text(
                    "${st.countIn(currentPref)} / ${Spots.inPref(currentPref).size}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.size(8.dp))
        // 地方
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (location != null) {
                ToggleButton(NEARBY, currentPref == NEARBY) { pref = NEARBY }
            }
            Spots.regions.forEach { r ->
                ToggleButton(r.name, currentPref != NEARBY && currentRegion == r.name) {
                    region = r.name
                    pref = r.prefs.first()
                }
            }
        }
        // 都道府県
        if (currentPref != NEARBY) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Spots.regions.first { it.name == currentRegion }.prefs.forEach { p ->
                    val done = st.countIn(p)
                    ToggleButton(p + if (done == Spots.inPref(p).size) " 👑" else "", p == currentPref) { pref = p }
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(spots, key = { it.id }) { spot ->
                SpotRow(spot, spot.id in st.visitedIds, location, showPref = currentPref == NEARBY, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun SpotRow(spot: Spot, visited: Boolean, location: Location?, showPref: Boolean, onOpen: (String) -> Unit) {
    val near = Spots.isNear(spot, location)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(spot.id) },
        colors = CardDefaults.cardColors(
            containerColor = when {
                near -> MaterialTheme.colorScheme.secondaryContainer
                visited -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(spot.emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(spot.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(spot.desc, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val extra = listOfNotNull(
                    if (showPref) spot.pref else null,
                    location?.let { formatDistance(Spots.distanceTo(spot, it)) },
                )
                if (extra.isNotEmpty()) Text(extra.joinToString("・"), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                when {
                    visited -> "✅"
                    near -> "📷"
                    else -> ""
                },
                fontSize = 22.sp,
            )
        }
    }
}
