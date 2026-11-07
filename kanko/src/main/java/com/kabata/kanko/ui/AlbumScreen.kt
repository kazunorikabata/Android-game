package com.kabata.kanko.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kabata.kanko.data.VisitRepo
import com.kabata.kanko.data.Spots
import com.kabata.kanko.data.Visit
import java.io.File

@Composable
fun AlbumScreen(st: VisitRepo.State) {
    var viewing by remember { mutableStateOf<Visit?>(null) }
    var confirmDelete by remember { mutableStateOf<Visit?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Text("旅のアルバム", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("写真 ${st.visits.size} 枚", style = MaterialTheme.typography.bodySmall)
        if (st.visits.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("まだ写真がありません。\nスポットで写真をとると、ここに集まります。", textAlign = TextAlign.Center)
            }
            return@Column
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(st.visits.sortedByDescending { it.time }, key = { it.photo }) { v ->
                val spot = Spots.byId(v.spotId)
                Card(Modifier.clickable { viewing = v }) {
                    AsyncImage(
                        model = File(v.photo), contentDescription = spot?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                    Column(Modifier.padding(8.dp)) {
                        Text(
                            "${spot?.emoji ?: ""} ${spot?.name ?: ""}", fontWeight = FontWeight.Bold, maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            formatDate(v.time) + if (v.onSite) "" else "（過去の写真）",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }

    viewing?.let { v ->
        val spot = Spots.byId(v.spotId)
        AlertDialog(
            onDismissRequest = { viewing = null },
            confirmButton = { TextButton(onClick = { viewing = null }) { Text("とじる") } },
            dismissButton = {
                TextButton(onClick = { confirmDelete = v; viewing = null }) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
            },
            title = { Text("${spot?.emoji ?: ""} ${spot?.name ?: ""}") },
            text = {
                Column {
                    AsyncImage(
                        model = File(v.photo), contentDescription = spot?.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(formatDate(v.time) + if (v.onSite) "・現地で撮影" else "・過去の写真から登録")
                }
            },
        )
    }

    confirmDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            confirmButton = {
                TextButton(onClick = { VisitRepo.deleteVisit(v); confirmDelete = null }) {
                    Text("削除する", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("やめる") } },
            title = { Text("この記録を削除しますか？") },
            text = { Text("写真も消えます。その場所の記録がほかになければ、未訪問にもどります。") },
        )
    }
}
