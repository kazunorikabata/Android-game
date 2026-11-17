package com.kabata.sangoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kabata.sangoku.model.BattleResult
import com.kabata.sangoku.model.City
import com.kabata.sangoku.model.Develop
import com.kabata.sangoku.model.Engine
import com.kabata.sangoku.model.Game
import com.kabata.sangoku.model.General
import com.kabata.sangoku.model.formatTroops

private val NeutralColor = Color(0xFF9E9E9E)
private val MapBg = Color(0xFFEDE0BE)
private val InkColor = Color(0xFF3B2A14)

private fun Game.colorOf(owner: Int) = if (owner >= 0) Color(factions[owner].color) else NeutralColor

/** 実行中のコマンド */
private sealed class Cmd {
    data class Dev(val kind: Develop) : Cmd()
    data object Draft : Cmd()
    data object Recruit : Cmd()
    data object Move : Cmd()
    data object Attack : Cmd()
}

@Composable
fun MainScreen(holder: GameHolder, g: Game) {
    // tick を読むことで、ゲームの中身が変わったときに描き直される
    val tick = holder.tick
    var selected by remember { mutableIntStateOf(g.citiesOf(g.player).firstOrNull()?.id ?: 0) }
    var cmd by remember { mutableStateOf<Cmd?>(null) }
    var message by remember { mutableStateOf<List<String>?>(null) }
    var battle by remember { mutableStateOf<BattleResult?>(null) }
    var captives by remember { mutableStateOf<List<General>>(emptyList()) }
    var report by remember { mutableStateOf<List<String>?>(null) }
    var confirmEnd by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val result = Engine.checkEnd(g)

    val city = g.cities[selected]
    val mine = g.citiesOf(g.player)

    Column(Modifier.fillMaxSize()) {
        // 上部の情報
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${g.year}年 ${g.month}月　${g.factionName(g.player)}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "都市 ${mine.size}/${g.cities.size}・兵 ${formatTroops(mine.sumOf { it.troops })}・" +
                        "金 ${mine.sumOf { it.gold }}・武将 ${g.generalsOf(g.player).size}",
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
            TextButton(onClick = { showMenu = true }) { Text("☰", color = Color.White, fontSize = 20.sp) }
            Button(
                onClick = { confirmEnd = true },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) { Text("ターン終了") }
        }

        MapView(g, tick, selected) { selected = it }

        CityPanel(
            g = g,
            city = city,
            tick = tick,
            modifier = Modifier.weight(1f),
            onCommand = { cmd = it },
            onSelectCity = { selected = it },
        )
    }

    // ---- コマンドの流れ ----
    cmd?.let { c ->
        CommandFlow(
            g = g,
            city = city,
            cmd = c,
            onCancel = { cmd = null },
            onMessage = { msg ->
                cmd = null
                holder.changed()
                message = listOf(msg)
            },
            onBattle = { r ->
                cmd = null
                holder.changed()
                battle = r
            },
        )
    }

    message?.let { lines ->
        AlertDialog(
            onDismissRequest = { message = null },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } },
            text = { Text(lines.joinToString("\n")) },
        )
    }

    battle?.let { r ->
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(onClick = {
                    battle = null
                    if (r.won) {
                        selected = r.city.id
                        captives = r.captured
                    }
                }) { Text("OK") }
            },
            title = { Text(if (r.won) "🏯 勝利！" else "🛡 攻略失敗", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    r.lines.forEach { Text(it) }
                }
            },
        )
    }

    // とらえた武将を1人ずつ処遇する
    if (battle == null && captives.isNotEmpty()) {
        val cap = captives.first()
        val here = g.cities[cap.city]
        val persuader = g.generalsOf(g.player).maxByOrNull { it.intel }
        val chance = persuader?.let { (Engine.recruitChance(it, cap) + 10).coerceAtMost(95) } ?: 0
        AlertDialog(
            onDismissRequest = {},
            title = { Text("${cap.name}をとらえた") },
            text = {
                Text(
                    "武力 ${cap.war}　知力 ${cap.intel}　政治 ${cap.pol}\n\n" +
                        "仲間になるよう説得しますか？" + (persuader?.let { "（説得役：${it.name}　成功率 約$chance%）" } ?: ""),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = (0 until 100).random() < chance
                    captives = captives.drop(1)
                    if (ok) {
                        cap.faction = g.player
                        cap.city = here.id
                        cap.acted = true
                        message = listOf("${cap.name}「この命、あなたにお預けしましょう」", "${cap.name}が仲間になった！")
                    } else {
                        message = listOf("${cap.name}「二君には仕えぬ」", "${cap.name}は去っていった（在野になりました）")
                    }
                    holder.changed()
                }, enabled = persuader != null) { Text("説得する") }
            },
            dismissButton = {
                TextButton(onClick = {
                    captives = captives.drop(1)
                    message = listOf("${cap.name}を解放した（在野になりました）")
                    holder.changed()
                }) { Text("解放する") }
            },
        )
    }

    if (confirmEnd) {
        val idle = g.generalsOf(g.player).count { !it.acted }
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    val r = Engine.endTurn(g)
                    holder.changed()
                    report = r
                }) { Text("ターン終了") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("もどる") } },
            title = { Text("${g.year}年${g.month}月を終えますか？") },
            text = { Text(if (idle > 0) "まだ行動していない武将が $idle 人います。" else "全員行動しました。") },
        )
    }

    report?.let { lines ->
        if (result == null) {
            AlertDialog(
                onDismissRequest = { report = null },
                confirmButton = { TextButton(onClick = { report = null }) { Text("OK") } },
                title = { Text("${g.year}年${g.month}月　各地の情勢") },
                text = {
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (lines.isEmpty()) Text("大きな動きはなかった。")
                        lines.forEach { Text(it) }
                        Text("（金と兵糧が入りました）", style = MaterialTheme.typography.bodySmall)
                    }
                },
            )
        }
    }

    if (result != null && battle == null && captives.isEmpty()) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = { TextButton(onClick = { holder.quit(deleteSave = true) }) { Text("タイトルへ") } },
            title = { Text(if (result == "win") "🎉 天下統一！" else "落城…", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (result == "win") "${g.year}年${g.month}月、${g.factionName(g.player)}はついに中華を統一した！\nあなたの名は永く歴史に刻まれるだろう。"
                    else "すべての都市を失い、${g.factionName(g.player)}は滅亡した…",
                )
            },
        )
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            confirmButton = { TextButton(onClick = { showMenu = false }) { Text("とじる") } },
            title = { Text("メニュー") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showMenu = false; showHelp = true }, modifier = Modifier.fillMaxWidth()) { Text("❓ あそびかた") }
                    OutlinedButton(onClick = { showMenu = false; holder.quit(deleteSave = false) }, modifier = Modifier.fillMaxWidth()) {
                        Text("💾 保存してタイトルへ")
                    }
                }
            },
        )
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("OK") } },
            title = { Text("あそびかた") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("・地図の都市をタップすると、下に情報が出ます。")
                    Text("・自分の都市では、武将1人につき1ターンに1回コマンドを実行できます。")
                    Text("・開墾（農業↑＝毎月の兵糧）、商業（商業↑＝毎月の金）、築城（防御↑＝守りが強くなる）")
                    Text("・徴兵で兵を集めます（金が必要）。兵は毎月兵糧を食べます。兵糧がなくなると兵が逃げます。")
                    Text("・登用：都市にいる在野の武将を仲間にさそいます。知力・政治が高い武将ほど成功しやすい。")
                    Text("・移動：となりの自分の都市へ武将と兵を動かします。")
                    Text("・出陣：となりの敵の都市や空白地を攻めます。武力は戦いの強さ、知力は計略に影響します。一騎討ちが起きることも。")
                    Text("・城を落とすと、守っていた武将をとらえることがあります。説得すれば仲間になるかも。")
                    Text("・ターン終了すると、ほかの勢力が動き、1か月が過ぎます。")
                    Text("・すべての都市を手に入れれば天下統一！ゲームは自動で保存されます。")
                }
            },
        )
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
private fun MapView(g: Game, tick: Int, selected: Int, onSelect: (Int) -> Unit) {
    val measurer = rememberTextMeasurer()
    val currentOnSelect by rememberUpdatedState(onSelect)
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1000f / 900f)
            .background(MapBg)
            .pointerInput(g) {
                detectTapGestures { pos ->
                    val sx = size.width / 1000f
                    val sy = size.height / 900f
                    val nearest = g.cities.minByOrNull { c ->
                        val dx = c.x * sx - pos.x
                        val dy = c.y * sy - pos.y
                        dx * dx + dy * dy
                    }
                    if (nearest != null) currentOnSelect(nearest.id)
                }
            },
    ) {
        // tick を参照して、内容が変わったら描き直す
        if (tick < 0) return@Canvas
        val sx = size.width / 1000f
        val sy = size.height / 900f
        val r = size.width / 48f
        g.cities.forEach { c ->
            c.neighbors.filter { it > c.id }.forEach { n ->
                val o = g.cities[n]
                drawLine(Color(0x66704020), Offset(c.x * sx, c.y * sy), Offset(o.x * sx, o.y * sy), strokeWidth = 3f)
            }
        }
        val selCity = g.cities[selected]
        selCity.neighbors.forEach { n ->
            val o = g.cities[n]
            drawLine(Color(0xCC8E2C2C), Offset(selCity.x * sx, selCity.y * sy), Offset(o.x * sx, o.y * sy), strokeWidth = 5f)
        }
        g.cities.forEach { c ->
            val center = Offset(c.x * sx, c.y * sy)
            if (c.id == selected) drawCircle(Color.Black, r * 1.5f, center)
            drawCircle(g.colorOf(c.owner), r, center)
            if (c.owner == g.player) drawCircle(Color.White, r * 0.45f, center)
            drawCircle(Color(0x99000000), r, center, style = Stroke(2f))
            val name = measurer.measure(
                c.name,
                TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = InkColor),
            )
            drawText(name, topLeft = Offset(center.x - name.size.width / 2f, center.y + r + 1f))
        }
    }
}

@Composable
private fun CityPanel(
    g: Game,
    city: City,
    tick: Int,
    modifier: Modifier,
    onCommand: (Cmd) -> Unit,
    onSelectCity: (Int) -> Unit,
) {
    val own = city.owner == g.player
    val gens = g.generalsIn(city).sortedByDescending { it.total }
    val free = g.freeIn(city)
    val idle = gens.filter { !it.acted }

    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(g.colorOf(city.owner), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(city.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(g.factionName(city.owner) + if (tick < 0) "" else "")
        }
        // 数値
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row { Stat("兵", formatTroops(city.troops)); Stat("金", "${city.gold}"); Stat("兵糧", "${city.food}") }
                Row { Stat("農業", "${city.agri}"); Stat("商業", "${city.comm}"); Stat("防御", "${city.defense}") }
            }
        }
        // コマンド
        if (own) {
            val hasIdle = idle.isNotEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CmdButton("🌾開墾", hasIdle, Modifier.weight(1f)) { onCommand(Cmd.Dev(Develop.AGRI)) }
                CmdButton("💰商業", hasIdle, Modifier.weight(1f)) { onCommand(Cmd.Dev(Develop.COMM)) }
                CmdButton("🧱築城", hasIdle, Modifier.weight(1f)) { onCommand(Cmd.Dev(Develop.WALL)) }
                CmdButton("🪖徴兵", hasIdle, Modifier.weight(1f)) { onCommand(Cmd.Draft) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CmdButton("📜登用", hasIdle && free.isNotEmpty(), Modifier.weight(1f)) { onCommand(Cmd.Recruit) }
                CmdButton("🐎移動", hasIdle && city.neighbors.any { g.cities[it].owner == g.player }, Modifier.weight(1f)) {
                    onCommand(Cmd.Move)
                }
                CmdButton("⚔出陣", hasIdle && city.troops > 0 && city.neighbors.any { g.cities[it].owner != g.player }, Modifier.weight(1f)) {
                    onCommand(Cmd.Attack)
                }
            }
            if (!hasIdle && gens.isNotEmpty()) Text("この都市の武将は全員行動しました", style = MaterialTheme.typography.bodySmall)
            if (gens.isEmpty()) Text("武将がいないため、コマンドを実行できません", style = MaterialTheme.typography.bodySmall)
        }
        // 武将
        Text("武将（${gens.size}）", fontWeight = FontWeight.Bold)
        if (gens.isEmpty()) Text("なし", style = MaterialTheme.typography.bodySmall)
        gens.forEach { gen -> GeneralRow(g, gen) }
        if (free.isNotEmpty()) {
            Text("在野の人物", fontWeight = FontWeight.Bold)
            free.forEach { gen -> GeneralRow(g, gen) }
        }
        // となりの都市
        Text("となりの都市", fontWeight = FontWeight.Bold)
        city.neighbors.map { g.cities[it] }.forEach { n ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelectCity(n.id) }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).background(g.colorOf(n.owner), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("${n.name}（${g.factionName(n.owner)}・兵${formatTroops(n.troops)}）", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(label: String, value: String) {
    Column(Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CmdButton(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
    ) { Text(label, fontSize = 13.sp) }
}

@Composable
private fun GeneralRow(g: Game, gen: General, trailing: String = "") {
    val ruler = gen.faction >= 0 && g.factions[gen.faction].ruler == gen.id
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            (if (ruler) "👑" else "") + gen.name,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(96.dp),
        )
        Text("武${gen.war} 知${gen.intel} 政${gen.pol}", fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            if (trailing.isNotEmpty()) trailing else if (gen.faction >= 0 && gen.acted) "済" else "",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

// ---------------- コマンドの流れ ----------------

@Composable
private fun CommandFlow(
    g: Game,
    city: City,
    cmd: Cmd,
    onCancel: () -> Unit,
    onMessage: (String) -> Unit,
    onBattle: (BattleResult) -> Unit,
) {
    val idle = g.generalsIn(city).filter { !it.acted }.sortedByDescending { it.total }
    var chosen by remember { mutableStateOf<List<General>>(emptyList()) }
    var recruitTarget by remember { mutableStateOf<General?>(null) }
    var target by remember { mutableStateOf<City?>(null) }

    when (cmd) {
        is Cmd.Dev -> GeneralPicker(
            title = "${cmd.kind.label}（金${cmd.kind.cost}）：だれが行う？",
            gens = idle,
            info = { gen ->
                when (cmd.kind) {
                    Develop.AGRI, Develop.COMM -> "効果 約+${10 + gen.pol / 4}"
                    Develop.WALL -> "効果 約+${8 + gen.pol / 6 + gen.war / 10}"
                }
            },
            multi = false,
            onCancel = onCancel,
        ) { onMessage(Engine.develop(g, it.first(), city, cmd.kind)) }

        Cmd.Draft -> GeneralPicker(
            title = "徴兵：だれが行う？（金 ${city.gold}）",
            gens = idle,
            info = { gen -> "兵+${Engine.draftAmount(gen)} 金-${Engine.draftCost(gen)}" },
            multi = false,
            onCancel = onCancel,
        ) { onMessage(Engine.draft(g, it.first(), city)) }

        Cmd.Recruit -> {
            val t = recruitTarget
            if (t == null) {
                GeneralPicker(
                    title = "登用：だれをさそう？",
                    gens = g.freeIn(city),
                    info = { "" },
                    multi = false,
                    onCancel = onCancel,
                ) { recruitTarget = it.first() }
            } else {
                GeneralPicker(
                    title = "${t.name}をだれが説得する？",
                    gens = idle,
                    info = { gen -> "成功率 約${Engine.recruitChance(gen, t)}%" },
                    multi = false,
                    onCancel = onCancel,
                ) {
                    val (_, msg) = Engine.recruit(g, it.first(), t, g.player, city)
                    onMessage(msg)
                }
            }
        }

        Cmd.Move, Cmd.Attack -> {
            val attack = cmd == Cmd.Attack
            val tgt = target
            when {
                chosen.isEmpty() -> GeneralPicker(
                    title = if (attack) "出陣：武将をえらぶ（3人まで）" else "移動：武将をえらぶ",
                    gens = idle,
                    info = { "" },
                    multi = true,
                    max = if (attack) 3 else 10,
                    onCancel = onCancel,
                ) { chosen = it }
                tgt == null -> CityPicker(
                    title = if (attack) "どこを攻める？" else "どこへ移動する？",
                    g = g,
                    cities = city.neighbors.map { g.cities[it] }
                        .filter { if (attack) it.owner != g.player else it.owner == g.player },
                    onCancel = onCancel,
                ) { target = it }
                else -> TroopPicker(
                    title = if (attack) "${tgt.name}へ何人つれていく？" else "${tgt.name}へ何人つれていく？",
                    max = city.troops,
                    min = if (attack) minOf(1000, city.troops) else 0,
                    hint = if (attack) "敵の兵 ${formatTroops(tgt.troops)}・防御 ${tgt.defense}" else "",
                    onCancel = onCancel,
                ) { n ->
                    if (attack) onBattle(Engine.battle(g, chosen, city, tgt, n))
                    else onMessage(Engine.move(g, chosen, city, tgt, n))
                }
            }
        }
    }
}

@Composable
private fun GeneralPicker(
    title: String,
    gens: List<General>,
    info: (General) -> String,
    multi: Boolean,
    max: Int = 1,
    onCancel: () -> Unit,
    onDone: (List<General>) -> Unit,
) {
    var picked by remember { mutableStateOf<List<General>>(emptyList()) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontSize = 18.sp) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(gens, key = { it.id }) { gen ->
                    val on = gen in picked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!multi) {
                                    onDone(listOf(gen))
                                } else {
                                    picked = if (on) picked - gen else if (picked.size < max) picked + gen else picked
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (multi) Checkbox(checked = on, onCheckedChange = null)
                        Column(Modifier.padding(start = 6.dp)) {
                            Text(gen.name, fontWeight = FontWeight.Bold)
                            Text("武${gen.war} 知${gen.intel} 政${gen.pol}  ${info(gen)}", fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (multi) {
                TextButton(onClick = { onDone(picked) }, enabled = picked.isNotEmpty()) { Text("決定（${picked.size}人）") }
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("やめる") } },
    )
}

@Composable
private fun CityPicker(title: String, g: Game, cities: List<City>, onCancel: () -> Unit, onDone: (City) -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontSize = 18.sp) },
        text = {
            Column {
                cities.forEach { c ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onDone(c) }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).background(g.colorOf(c.owner), CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text(
                                "${g.factionName(c.owner)}・兵${formatTroops(c.troops)}・防御${c.defense}・武将${g.generalsIn(c).size}人",
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancel) { Text("やめる") } },
    )
}

@Composable
private fun TroopPicker(title: String, max: Int, min: Int, hint: String, onCancel: () -> Unit, onDone: (Int) -> Unit) {
    var value by remember { mutableFloatStateOf(if (min > 0) max * 0.8f else max / 2f) }
    val n = (value.toInt() / 100 * 100).coerceIn(min, max)
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title, fontSize = 18.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("兵 ${formatTroops(n)} / ${formatTroops(max)}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (max > min) Slider(value = value, onValueChange = { value = it }, valueRange = min.toFloat()..max.toFloat())
                if (hint.isNotEmpty()) Text(hint, style = MaterialTheme.typography.bodySmall)
                Text("残る兵 ${formatTroops(max - n)}", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
            }
        },
        confirmButton = { TextButton(onClick = { onDone(n) }) { Text("決定") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("やめる") } },
    )
}
