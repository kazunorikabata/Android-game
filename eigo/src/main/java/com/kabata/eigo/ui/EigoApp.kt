package com.kabata.eigo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabata.eigo.Game
import com.kabata.eigo.Progress
import com.kabata.eigo.Question

/** 遊んでいる1回分（ステージ or 復習） */
data class Session(
    val title: String,
    val questions: List<Question>,
    val level: Int?,
    val stage: Int?,
    /** 盤面を作るときの乱数の種（ステージは毎回同じ盤面になる） */
    val seed: Long,
)

fun stageSession(level: Int, stage: Int): Session {
    val name = Game.levels.first { it.no == level }.name
    return Session("$name ステージ${stage + 1}", Game.stage(level, stage), level, stage, level * 100L + stage)
}

/** まちがえた問題からふくしゅう用のクロスワードを作る（なければ null） */
fun reviewSession(): Session? {
    val qs = Progress.state.value.missed.mapNotNull { Game.byKey(it) }.shuffled().take(Game.STAGE_SIZE)
    return if (qs.isEmpty()) null else Session("ふくしゅう", qs, null, null, System.currentTimeMillis())
}

@Composable
fun EigoApp() {
    var session by remember { mutableStateOf<Session?>(null) }
    var round by remember { mutableStateOf(0) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            val s = session
            if (s == null) {
                HomeScreen(onStart = { session = it; round++ })
            } else {
                CrosswordScreen(
                    roundKey = round,
                    session = s,
                    onExit = { session = null },
                    onRestart = { next -> session = next; round++ },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(onStart: (Session) -> Unit) {
    val st by Progress.state.collectAsStateWithLifecycle()
    val maxStars = Game.levels.sumOf { Game.stageCount(it.no) } * 3

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("英語クロスワード", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("英文の空欄に入る単語で、クロスワードを完成させよう！", style = MaterialTheme.typography.bodyMedium)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("あつめた星", style = MaterialTheme.typography.labelLarge)
                    Text("★ ${st.totalStars} / $maxStars", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("まちがえた問題 ${st.missed.size}問", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = { reviewSession()?.let(onStart) },
                        enabled = st.missed.isNotEmpty(),
                    ) { Text("📝 ふくしゅう") }
                }
            }
        }

        Game.levels.forEach { level ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(level.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        level.desc,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val count = Game.stageCount(level.no)
                    (0 until count).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { stage ->
                                StageButton(
                                    stage = stage,
                                    stars = st.starsOf(level.no, stage),
                                    unlocked = Progress.isUnlocked(level.no, stage),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onStart(stageSession(level.no, stage)) },
                                )
                            }
                            repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
        Text(
            "ステージをクリアすると次のステージが開きます。ヒントや答え合わせのまちがいが少ないほど星が増えます。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StageButton(stage: Int, stars: Int, unlocked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.alpha(if (unlocked) 1f else 0.45f).clickable(enabled = unlocked, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stars > 0) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (unlocked) "${stage + 1}" else "🔒", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                "★".repeat(stars) + "☆".repeat(3 - stars),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.size(width = 60.dp, height = 20.dp),
            )
        }
    }
}
