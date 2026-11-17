package com.kabata.sangoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabata.sangoku.model.Game
import com.kabata.sangoku.model.formatTroops
import java.io.File

/** ゲームの状態を持ち、変更があったら画面を描き直して保存する */
class GameHolder(private val file: File) {
    var game by mutableStateOf<Game?>(null)
        private set
    var tick by mutableIntStateOf(0)
        private set

    fun hasSave() = file.exists()

    fun newGame(player: Int) {
        game = Game.newGame(player)
        changed()
    }

    fun load(): Boolean {
        val g = runCatching { Game.fromJson(file.readText()) }.getOrNull() ?: return false
        game = g
        tick++
        return true
    }

    fun changed() {
        tick++
        save()
    }

    fun save() {
        game?.let { runCatching { file.writeText(it.toJson()) } }
    }

    fun quit(deleteSave: Boolean) {
        if (deleteSave) file.delete() else save()
        game = null
    }
}

@Composable
fun SangokuApp(holder: GameHolder) {
    var selecting by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            val g = holder.game
            when {
                g != null -> MainScreen(holder, g)
                selecting -> SelectScreen(onBack = { selecting = false }, onSelect = { f ->
                    selecting = false
                    holder.newGame(f)
                })
                else -> TitleScreen(holder, onNew = { selecting = true })
            }
        }
    }
}

@Composable
private fun TitleScreen(holder: GameHolder, onNew: () -> Unit) {
    var failed by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(60.dp))
        Text("三国群雄伝", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("〜 天下統一への道 〜", fontSize = 18.sp)
        Text("西暦196年。漢王朝はおとろえ、各地の群雄が覇を競う。\nあなたは君主となり、中華の統一をめざせ。", textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("はじめから", fontSize = 18.sp) }
        OutlinedButton(
            onClick = { if (!holder.load()) failed = true },
            enabled = holder.hasSave(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("つづきから", fontSize = 18.sp) }
        if (failed) Text("セーブデータを読みこめませんでした", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(20.dp))
        Text(
            "※ 歴史をもとにしたオリジナルのゲームです。登場人物の能力値は独自のものです。",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SelectScreen(onBack: () -> Unit, onSelect: (Int) -> Unit) {
    val preview = remember { Game.newGame(0) }
    var confirm by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("◀ もどる") }
            Text("君主をえらぶ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        preview.factions.forEach { f ->
            val cities = preview.citiesOf(f.id)
            val gens = preview.generalsOf(f.id)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { confirm = f.id },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).background(Color(f.color), CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${f.name}軍", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("【${f.difficulty}】", color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
                        }
                        Text(f.desc, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "都市 ${cities.size}・武将 ${gens.size}・兵 ${formatTroops(cities.sumOf { it.troops })}" +
                                "　拠点：${cities.joinToString("・") { it.name }}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
    confirm?.let { f ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            confirmButton = { TextButton(onClick = { onSelect(f) }) { Text("この君主ではじめる") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("やめる") } },
            title = { Text("${preview.factions[f].name}軍ではじめますか？") },
            text = { Text("※ 前のセーブデータは上書きされます") },
        )
    }
}
