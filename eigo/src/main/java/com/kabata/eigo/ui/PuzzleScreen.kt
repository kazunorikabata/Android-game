package com.kabata.eigo.ui

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.kabata.eigo.Game
import com.kabata.eigo.Progress
import com.kabata.eigo.Question
import java.util.Locale

private data class Tile(val id: Int, val ch: Char)

private enum class Phase { Answering, Correct, Revealed }

private val CorrectColor = Color(0xFF2E7D32)
private const val DECOY_LETTERS = "abcdeghiklmnoprstuwy"

private fun makeTiles(q: Question): List<Tile> {
    val decoys = (1..Game.decoysFor(q.level)).map { DECOY_LETTERS.random() }
    var letters = (q.answer.toList() + decoys).shuffled()
    // 最初から正解の順番になっていたらもう一度まぜる
    repeat(5) { if (letters.take(q.answer.length).joinToString("") == q.answer) letters = letters.shuffled() }
    return letters.mapIndexed { i, c -> Tile(i, c) }
}

@Composable
fun PuzzleScreen(roundKey: Int, session: Session, onExit: () -> Unit, onRestart: (Session) -> Unit) {
    key(roundKey) { PuzzleContent(session, onExit, onRestart) }
}

@Composable
private fun PuzzleContent(session: Session, onExit: () -> Unit, onRestart: (Session) -> Unit) {
    BackHandler(onBack = onExit)
    val context = LocalContext.current
    val questions = session.questions

    // 読み上げ
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                tts = engine
            }
        }
        onDispose { engine?.shutdown() }
    }
    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eigo")
    }

    var qIndex by remember { mutableIntStateOf(0) }
    var penalty by remember { mutableIntStateOf(0) }
    var perfectCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    if (questions.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("問題がありません")
            Button(onClick = onExit) { Text("もどる") }
        }
        return
    }

    if (finished) {
        ResultView(session, penalty, perfectCount, onExit, onRestart)
        return
    }

    val q = questions[qIndex]
    val tiles = remember(qIndex) { makeTiles(q) }
    val slots = remember(qIndex) { mutableStateListOf<Int?>().apply { repeat(q.answer.length) { add(null) } } }
    var locked by remember(qIndex) { mutableIntStateOf(0) }
    var phase by remember(qIndex) { mutableStateOf(Phase.Answering) }
    var wrong by remember(qIndex) { mutableStateOf(false) }
    var qPenalty by remember(qIndex) { mutableIntStateOf(0) }

    val full = q.sentence.replace("___", q.answer)
    val used = slots.filterNotNull().toSet()

    fun addPenalty(n: Int) {
        qPenalty += n
        penalty += n
    }

    fun check() {
        val word = slots.map { id -> id?.let { tiles[it].ch } ?: ' ' }.joinToString("")
        if (word == q.answer) {
            phase = Phase.Correct
            if (qPenalty == 0) {
                perfectCount++
                Progress.markLearned(q)
            } else {
                Progress.markMissed(q)
            }
            speak(full)
        } else {
            wrong = true
            addPenalty(1)
        }
    }

    fun tapTile(t: Tile) {
        if (phase != Phase.Answering || t.id in used) return
        val idx = slots.indexOfFirst { it == null }
        if (idx < 0) return
        slots[idx] = t.id
        wrong = false
        if (slots.none { it == null }) check()
    }

    fun tapSlot(i: Int) {
        if (phase != Phase.Answering || i < locked) return
        slots[i] = null
        wrong = false
    }

    /** 先頭から n 文字を正しいタイルでうめて固定する */
    fun fillCorrect(n: Int) {
        for (i in slots.indices) slots[i] = null
        for (i in 0 until n) {
            val tile = tiles.first { it.ch == q.answer[i] && it.id !in slots }
            slots[i] = tile.id
        }
    }

    fun hint() {
        if (phase != Phase.Answering || locked >= q.answer.length - 1) return
        locked++
        addPenalty(1)
        fillCorrect(locked)
        wrong = false
    }

    fun skip() {
        if (phase != Phase.Answering) return
        fillCorrect(q.answer.length)
        addPenalty(3)
        Progress.markMissed(q)
        phase = Phase.Revealed
        speak(full)
    }

    fun next() {
        if (qIndex + 1 < questions.size) qIndex++ else finished = true
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕") }
            Text(session.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${qIndex + 1} / ${questions.size}", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { (qIndex + 1f) / questions.size }, modifier = Modifier.fillMaxWidth())

        // 問題文
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(q.ja, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val parts = q.sentence.split("___")
                val blankColor = if (phase == Phase.Answering) MaterialTheme.colorScheme.primary else CorrectColor
                Text(
                    buildAnnotatedString {
                        append(parts[0])
                        withStyle(SpanStyle(color = blankColor, fontWeight = FontWeight.Bold)) {
                            append(
                                slots.joinToString("") { id -> id?.let { tiles[it].ch.toString() } ?: "＿" },
                            )
                        }
                        append(parts.getOrElse(1) { "" })
                    },
                    fontSize = 22.sp,
                    lineHeight = 32.sp,
                )
            }
        }

        // 空欄のマス
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val n = q.answer.length
            val gap = 6.dp
            val cell = min(48.dp, (maxWidth - gap * (n - 1)) / n)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally)) {
                slots.forEachIndexed { i, id ->
                    val isLocked = i < locked
                    val border = when {
                        phase != Phase.Answering -> CorrectColor
                        wrong -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        Modifier
                            .size(cell)
                            .background(
                                when {
                                    phase != Phase.Answering -> CorrectColor.copy(alpha = 0.15f)
                                    isLocked -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                RoundedCornerShape(8.dp),
                            )
                            .border(BorderStroke(2.dp, border), RoundedCornerShape(8.dp))
                            .clickable { tapSlot(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            id?.let { tiles[it].ch.toString() } ?: "",
                            fontSize = (cell.value * 0.5f).sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        if (wrong) {
            Text(
                "❌ ちがうみたい… マスをタップすると文字をもどせます",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }

        if (phase == Phase.Answering) {
            // 文字タイル
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                        row.forEach { t ->
                            val isUsed = t.id in used
                            Box(
                                Modifier
                                    .size(50.dp)
                                    .alpha(if (isUsed) 0.2f else 1f)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isUsed) { tapTile(t) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    t.ch.toString(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = ::hint, modifier = Modifier.weight(1f)) { Text("💡 ヒント") }
                OutlinedButton(
                    onClick = {
                        for (i in locked until slots.size) slots[i] = null
                        wrong = false
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("↩ クリア") }
                OutlinedButton(onClick = ::skip, modifier = Modifier.weight(1f)) { Text("⏭ パス") }
            }
        } else {
            // 答え合わせ
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (phase == Phase.Correct) CorrectColor.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            phase == Phase.Revealed -> "答えは「${q.answer}」"
                            qPenalty == 0 -> "⭕ かんぺき！"
                            else -> "⭕ 正解！"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (phase == Phase.Correct) CorrectColor else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(full, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(q.ja, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { speak(full) }, modifier = Modifier.weight(1f)) { Text("🔊 よみあげ") }
                Button(onClick = ::next, modifier = Modifier.weight(1f)) {
                    Text(if (qIndex + 1 < questions.size) "つぎへ ▶" else "結果を見る")
                }
            }
        }
    }
}

@Composable
private fun ResultView(
    session: Session,
    penalty: Int,
    perfectCount: Int,
    onExit: () -> Unit,
    onRestart: (Session) -> Unit,
) {
    val level = session.level
    val stage = session.stage
    val stars = Game.starsFor(penalty)
    // 結果は一度だけ記録する
    val newRecord = remember { if (level != null && stage != null) Progress.recordStage(level, stage, stars) else false }
    val hasNext = level != null && stage != null && stage + 1 < Game.stageCount(level)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(session.title, fontWeight = FontWeight.Bold)
        if (level != null) {
            Text(
                "★".repeat(stars) + "☆".repeat(3 - stars),
                fontSize = 56.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Text("📝", fontSize = 56.sp)
        }
        Text(
            "ノーミス正解 $perfectCount / ${session.questions.size} 問",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            when {
                level == null -> "ノーミスで正解した問題は「まちがえた問題」から消えます"
                stars == 3 -> "パーフェクト！すばらしい！"
                stars == 2 -> "よくできました！"
                else -> "クリア！もう一度やると星が増えるかも"
            },
            textAlign = TextAlign.Center,
        )
        if (newRecord) Text("記録更新！", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (hasNext) {
            Button(onClick = { onRestart(stageSession(level!!, stage!! + 1)) }, modifier = Modifier.fillMaxWidth()) {
                Text("つぎのステージへ ▶")
            }
        }
        OutlinedButton(
            onClick = {
                if (level != null && stage != null) {
                    onRestart(stageSession(level, stage))
                } else {
                    val qs = Progress.state.value.missed.mapNotNull { Game.byKey(it) }.shuffled().take(Game.STAGE_SIZE)
                    if (qs.isEmpty()) onExit() else onRestart(session.copy(questions = qs))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("もう一度") }
        TextButton(onClick = onExit) { Text("ホームにもどる") }
    }
}
