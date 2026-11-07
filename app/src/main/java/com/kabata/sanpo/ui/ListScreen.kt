package com.kabata.sanpo.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabata.sanpo.data.Castle
import com.kabata.sanpo.data.CastleRepo
import com.kabata.sanpo.data.Castles

@Composable
fun ListScreen(st: CastleRepo.State, location: Location?, onOpen: (Int) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0:すべて 1:未登城 2:登城ずみ
    var byDistance by rememberSaveable { mutableIntStateOf(0) }

    val castles = Castles.all
        .filter {
            when (filter) {
                1 -> it.no !in st.visitedNos
                2 -> it.no in st.visitedNos
                else -> true
            }
        }
        .let { list ->
            if (byDistance == 1 && location != null) list.sortedBy { CastleRepo.distanceTo(it, location) } else list
        }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Text("百名城いちらん", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleButton("すべて", filter == 0) { filter = 0 }
            ToggleButton("未登城", filter == 1) { filter = 1 }
            ToggleButton("登城ずみ", filter == 2) { filter = 2 }
            if (location != null) {
                ToggleButton(if (byDistance == 1) "📍 近い順" else "🔢 番号順", true) {
                    byDistance = 1 - byDistance
                }
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var lastRegion: String? = null
            castles.forEach { castle ->
                val region = Castles.regionOf(castle.no).name
                if (byDistance == 0 && region != lastRegion) {
                    lastRegion = region
                    item(key = "region_$region") {
                        Text(
                            region,
                            modifier = Modifier.padding(top = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                item(key = castle.no) {
                    CastleRow(castle, castle.no in st.visitedNos, location, onOpen)
                }
            }
        }
    }
}

@Composable
private fun CastleRow(castle: Castle, visited: Boolean, location: Location?, onOpen: (Int) -> Unit) {
    val near = CastleRepo.isNear(castle, location)
    val best by com.kabata.sanpo.quiz.QuizRepo.best.collectAsStateWithLifecycle()
    val quizPerfect = best[castle.no]?.perfect == true
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(castle.no) },
        colors = CardDefaults.cardColors(
            containerColor = when {
                near -> MaterialTheme.colorScheme.secondaryContainer
                visited -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "%03d".format(castle.no),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(castle.name, fontWeight = FontWeight.Bold)
                Text(
                    castle.pref + (location?.let { "・" + formatDistance(CastleRepo.distanceTo(castle, it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (quizPerfect) Text("🎓", fontSize = 18.sp)
            Text(
                when {
                    visited -> "✅"
                    near -> "📷"
                    else -> "🏯"
                },
                fontSize = 22.sp,
            )
        }
    }
}
