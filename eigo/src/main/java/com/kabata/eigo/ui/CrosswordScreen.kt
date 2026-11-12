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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.kabata.eigo.CrosswordGenerator
import com.kabata.eigo.Entry
import com.kabata.eigo.Game
import com.kabata.eigo.Progress
import java.util.Locale

private typealias Cell = Pair<Int, Int>

private val CorrectColor = Color(0xFF2E7D32)
private val KEY_ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

@Composable
fun CrosswordScreen(roundKey: Int, session: Session, onExit: () -> Unit, onRestart: (Session) -> Unit) {
    key(roundKey) { CrosswordContent(session, onExit, onRestart) }
}

@Composable
private fun CrosswordContent(session: Session, onExit: () -> Unit, onRestart: (Session) -> Unit) {
    BackHandler(onBack = onExit)
    val context = LocalContext.current

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
    val speak: (String) -> Unit = { text -> tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "eigo") }

    val cw = remember { CrosswordGenerator.generate(session.questions, session.seed) }
    if (cw.entries.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("問題がありません")
            Button(onClick = onExit) { Text("もどる") }
        }
        return
    }

    val input = remember { mutableStateMapOf<Cell, Char>() }
    val revealed = remember { mutableStateMapOf<Cell, Boolean>() }
    val wrongCells = remember { mutableStateMapOf<Cell, Boolean>() }
    val missedWords = remember { mutableStateMapOf<Int, Boolean>() }
    var selected by remember { mutableIntStateOf(0) }
    var cursor by remember { mutableStateOf(cw.entries[0].cells[0]) }
    var penalty by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var finished by remember { mutableStateOf(false) }
    var gaveUp by remember { mutableStateOf(false) }
    var confirmGiveUp by remember { mutableStateOf(false) }
    var showClues by remember { mutableStateOf(false) }

    if (finished) {
        CrosswordResult(session, cw.entries, penalty, gaveUp, missedWords.keys, speak, onExit, onRestart)
        return
    }

    val entry = cw.entries[selected]

    fun checkComplete() {
        val allFilled = cw.solution.keys.all { input[it] != null }
        if (!allFilled) return
        if (cw.solution.all { (cell, ch) -> input[cell] == ch }) {
            finished = true
        } else {
            message = "どこかがちがいます。「🔍 答え合わせ」で確認できます"
        }
    }

    fun select(index: Int, at: Cell? = null) {
        selected = index
        val e = cw.entries[index]
        cursor = at ?: e.cells.firstOrNull { input[it] == null } ?: e.cells[0]
    }

    fun tapCell(cell: Cell) {
        val here = cw.entriesAt(cell)
        if (here.isEmpty()) return
        if (cell == cursor && here.size > 1) {
            // 同じマスをもう一度タップするとタテ・ヨコを切りかえ
            select(here.first { it != selected }, cell)
        } else if (selected in here) {
            cursor = cell
        } else {
            val sameDir = here.firstOrNull { cw.entries[it].across == entry.across }
            select(sameDir ?: here.first(), cell)
        }
    }

    fun type(ch: Char) {
        message = null
        if (revealed[cursor] != true) {
            input[cursor] = ch
            wrongCells.remove(cursor)
        }
        val cells = entry.cells
        val i = cells.indexOf(cursor)
        if (i in 0 until cells.size - 1) cursor = cells[i + 1]
        checkComplete()
    }

    fun backspace() {
        message = null
        val cells = entry.cells
        if (input[cursor] != null && revealed[cursor] != true) {
            input.remove(cursor)
            wrongCells.remove(cursor)
            return
        }
        val i = cells.indexOf(cursor)
        if (i > 0) {
            cursor = cells[i - 1]
            if (revealed[cursor] != true) {
                input.remove(cursor)
                wrongCells.remove(cursor)
            }
        }
    }

    fun hint() {
        val ch = cw.solution[cursor] ?: return
        if (revealed[cursor] == true) return
        input[cursor] = ch
        revealed[cursor] = true
        wrongCells.remove(cursor)
        penalty++
        cw.entriesAt(cursor).forEach { missedWords[it] = true }
        val cells = entry.cells
        val i = cells.indexOf(cursor)
        if (i in 0 until cells.size - 1) cursor = cells[i + 1]
        checkComplete()
    }

    fun check() {
        val wrong = cw.solution.filter { (cell, ch) -> input[cell] != null && input[cell] != ch }.keys
        wrongCells.clear()
        wrong.forEach { wrongCells[it] = true }
        if (wrong.isEmpty()) {
            message = "ここまでまちがいなし！"
        } else {
            penalty++
            wrong.forEach { cell -> cw.entriesAt(cell).forEach { missedWords[it] = true } }
            message = "赤いマスがちがいます（${wrong.size}マス）"
        }
    }

    fun giveUp() {
        cw.entries.forEachIndexed { i, e -> if (e.cells.any { input[it] != cw.solution[it] }) missedWords[i] = true }
        cw.solution.forEach { (cell, ch) -> input[cell] = ch }
        gaveUp = true
        penalty += 10
        finished = true
    }

    Column(Modifier.fillMaxSize()) {
        // 上部：タイトル
        Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕") }
            Text(session.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${cw.entries.size}語", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.size(8.dp))
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 盤面
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = min(40.dp, maxWidth / cw.cols)
                val selectedCells = entry.cells.toSet()
                Column(Modifier.align(Alignment.Center)) {
                    for (r in 0 until cw.rows) {
                        Row {
                            for (c in 0 until cw.cols) {
                                val pos = r to c
                                val sol = cw.solution[pos]
                                if (sol == null) {
                                    Box(Modifier.size(cell))
                                } else {
                                    val bg = when {
                                        pos == cursor -> MaterialTheme.colorScheme.secondary
                                        wrongCells[pos] == true -> MaterialTheme.colorScheme.errorContainer
                                        pos in selectedCells -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                    Box(
                                        Modifier
                                            .size(cell)
                                            .background(bg)
                                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                                            .clickable { tapCell(pos) },
                                    ) {
                                        cw.numbers[pos]?.let { n ->
                                            Text(
                                                "$n",
                                                fontSize = (cell.value * 0.26f).sp,
                                                lineHeight = (cell.value * 0.28f).sp,
                                                modifier = Modifier.padding(start = 2.dp),
                                            )
                                        }
                                        Text(
                                            input[pos]?.toString() ?: "",
                                            modifier = Modifier.align(Alignment.Center),
                                            fontSize = (cell.value * 0.55f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                revealed[pos] == true -> MaterialTheme.colorScheme.primary
                                                wrongCells[pos] == true -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            // 今のヒント
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { select((selected - 1 + cw.entries.size) % cw.entries.size) }) { Text("◀") }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${entry.number}. ${if (entry.across) "ヨコ" else "タテ"}（${entry.word.length}文字）",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(entry.q.sentence, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(entry.q.ja, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { select((selected + 1) % cw.entries.size) }) { Text("▶") }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = ::hint, modifier = Modifier.weight(1f)) { Text("💡1文字") }
                OutlinedButton(onClick = ::check, modifier = Modifier.weight(1f)) { Text("🔍答え合わせ") }
                OutlinedButton(onClick = { showClues = !showClues }, modifier = Modifier.weight(1f)) {
                    Text(if (showClues) "📋とじる" else "📋一覧")
                }
            }

            if (showClues) {
                ClueList(cw.entries, selected, input, cw.solution) { select(it); showClues = false }
                TextButton(onClick = { confirmGiveUp = true }) { Text("🏳 降参して答えを見る") }
            }
        }

        // キーボード
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            KEY_ROWS.forEachIndexed { rowIndex, keys ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (rowIndex == 1) Spacer(Modifier.weight(0.5f))
                    keys.forEach { ch -> Key(ch.toString(), Modifier.weight(1f)) { type(ch) } }
                    if (rowIndex == 1) Spacer(Modifier.weight(0.5f))
                    if (rowIndex == 2) {
                        Key("⇄", Modifier.weight(1.5f)) {
                            cw.entriesAt(cursor).firstOrNull { it != selected }?.let { select(it, cursor) }
                        }
                        Key("⌫", Modifier.weight(1.5f)) { backspace() }
                    }
                }
            }
        }
    }

    if (confirmGiveUp) {
        AlertDialog(
            onDismissRequest = { confirmGiveUp = false },
            confirmButton = { TextButton(onClick = { confirmGiveUp = false; giveUp() }) { Text("降参する") } },
            dismissButton = { TextButton(onClick = { confirmGiveUp = false }) { Text("もう少しがんばる") } },
            title = { Text("答えを見ますか？") },
            text = { Text("星は1つになります。") },
        )
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ClueList(
    entries: List<Entry>,
    selected: Int,
    input: Map<Cell, Char>,
    solution: Map<Cell, Char>,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(true to "ヨコのカギ", false to "タテのカギ").forEach { (across, title) ->
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            entries.withIndex().filter { it.value.across == across }.forEach { (i, e) ->
                val done = e.cells.all { input[it] == solution[it] }
                Text(
                    "${e.number}. ${e.q.sentence}" + if (done) "  ✅" else "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (i == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            RoundedCornerShape(6.dp),
                        )
                        .clickable { onSelect(i) }
                        .padding(6.dp),
                )
            }
        }
    }
}

@Composable
private fun CrosswordResult(
    session: Session,
    entries: List<Entry>,
    penalty: Int,
    gaveUp: Boolean,
    missed: Set<Int>,
    speak: (String) -> Unit,
    onExit: () -> Unit,
    onRestart: (Session) -> Unit,
) {
    val level = session.level
    val stage = session.stage
    val stars = if (gaveUp) 1 else Game.starsFor(penalty)
    // 結果は一度だけ記録する
    val newRecord = remember {
        entries.forEachIndexed { i, e -> if (i in missed) Progress.markMissed(e.q) else Progress.markLearned(e.q) }
        if (level != null && stage != null) Progress.recordStage(level, stage, stars) else false
    }
    val hasNext = level != null && stage != null && stage + 1 < Game.stageCount(level)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(session.title, fontWeight = FontWeight.Bold)
        if (level != null) {
            Text("★".repeat(stars) + "☆".repeat(3 - stars), fontSize = 52.sp, color = MaterialTheme.colorScheme.secondary)
        } else {
            Text("📝", fontSize = 52.sp)
        }
        Text(
            when {
                gaveUp -> "答えを確認しよう"
                stars == 3 -> "パーフェクト！ノーヒントで完成！"
                stars == 2 -> "完成！よくできました！"
                else -> "完成！もう一度やると星が増えるかも"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (newRecord) Text("記録更新！", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        if (hasNext) {
            Button(onClick = { onRestart(stageSession(level!!, stage!! + 1)) }, modifier = Modifier.fillMaxWidth()) {
                Text("つぎのステージへ ▶")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (level != null && stage != null) onRestart(stageSession(level, stage))
                    else reviewSession()?.let(onRestart) ?: onExit()
                },
                modifier = Modifier.weight(1f),
            ) { Text("もう一度") }
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text("ホームへ") }
        }

        Text("今回の単語", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        entries.forEachIndexed { i, e ->
            val full = e.q.sentence.replace("___", e.word)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (i in missed) MaterialTheme.colorScheme.secondaryContainer
                    else CorrectColor.copy(alpha = 0.10f),
                ),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${e.word}" + if (i in missed) "（ふくしゅうへ）" else "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(full)
                        Text(e.q.ja, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { speak(full) }) { Text("🔊", fontSize = 22.sp) }
                }
            }
        }
    }
}
