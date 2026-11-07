package com.kabata.sanpo.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kabata.sanpo.BuildConfig
import com.kabata.sanpo.GameState
import com.kabata.sanpo.Journey
import com.kabata.sanpo.Pet
import com.kabata.sanpo.SanpoViewModel
import com.kabata.sanpo.Spot
import com.kabata.sanpo.progressBetween

private fun fmt(n: Long) = "%,d".format(n)

@Composable
fun SanpoApp(vm: SanpoViewModel, permissionGranted: Boolean, onRequestPermission: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("🐾", fontSize = 20.sp) }, label = { Text("ホーム") },
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Text("🧭", fontSize = 20.sp) }, label = { Text("ちず") },
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Text("📷", fontSize = 20.sp) }, label = { Text("めいしょ") },
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Text("🗺️", fontSize = 20.sp) }, label = { Text("きろく") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeScreen(
                    state = state,
                    hasSensor = vm.hasStepSensor,
                    permissionGranted = permissionGranted,
                    onRequestPermission = onRequestPermission,
                    onDebugSteps = vm::addDebugSteps,
                )
                1 -> MapScreen()
                2 -> MeishoScreen()
                else -> JourneyScreen(state)
            }
        }
    }

    if (state.hasNewArrival) {
        ArrivalDialog(Journey.spots[state.spotIndex], onDismiss = vm::acknowledgeArrival)
    }
}

@Composable
private fun HomeScreen(
    state: GameState,
    hasSensor: Boolean,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onDebugSteps: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("てくてくさんぽ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        when {
            !hasSensor -> Notice("この端末には歩数センサーがありません。下のテストボタンで遊べます。")
            !permissionGranted -> Notice(
                "歩数を数えるには「身体活動」の許可が必要です。",
                action = "許可する",
                onAction = onRequestPermission,
            )
        }

        TodayCard(state)
        PetCard(state)
        RoadCard(state)
        MeishoSummaryCard()

        if (BuildConfig.DEBUG || !hasSensor) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onDebugSteps(100) }) { Text("テスト +100歩") }
                OutlinedButton(onClick = { onDebugSteps(1_000) }) { Text("テスト +1000歩") }
            }
        }
    }
}

@Composable
private fun MeishoSummaryCard() {
    val st by com.kabata.sanpo.meisho.MeishoRepo.state.collectAsStateWithLifecycle()
    SectionCard("めいしょめぐり") {
        Text(
            "⭐ ${st.totalPoints} pt",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            "おとずれた名所 ${st.visitedIds.size} か所・写真 ${st.visits.size} 枚",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun Notice(message: String, action: String? = null, onAction: () -> Unit = {}) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message)
            if (action != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun TodayCard(state: GameState) {
    val target = (state.todaySteps.toFloat() / state.dailyGoal).coerceIn(0f, 1f)
    val progress by animateFloatAsState(target, tween(800), label = "today")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bar = MaterialTheme.colorScheme.primary

    SectionCard("きょうの歩数") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 18.dp.toPx()
                    val topLeft = Offset(stroke / 2, stroke / 2)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                    drawArc(
                        bar, -90f, 360f * progress, false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmt(state.todaySteps), fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text("/ ${fmt(state.dailyGoal)} 歩", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        val remaining = state.dailyGoal - state.todaySteps
        Text(
            if (remaining <= 0) "きょうの目標たっせい！🎉" else "目標まであと ${fmt(remaining)} 歩",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "目標たっせい日数：${state.goalDays} 日　／　これまでの合計：${fmt(state.totalSteps)} 歩",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PetCard(state: GameState) {
    val index = Pet.stageIndexFor(state.totalSteps)
    val stage = Pet.stages[index]
    val next = Pet.stages.getOrNull(index + 1)

    val bounce = rememberInfiniteTransition(label = "pet")
    val dy by bounce.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dy",
    )

    SectionCard("あいぼう") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stage.emoji, fontSize = 64.sp, modifier = Modifier.graphicsLayer { translationY = dy })
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${Pet.NAME}（${stage.name}）", fontWeight = FontWeight.Bold)
                if (next != null) {
                    val p = progressBetween(state.totalSteps, stage.steps, next.steps)
                    LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth())
                    Text(
                        "あと ${fmt(next.steps - state.totalSteps)} 歩で「${next.name}」にせいちょう",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    Text("これ以上ないくらい立派に育った！", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RoadCard(state: GameState) {
    val index = state.spotIndex
    val here = Journey.spots[index]
    val next = Journey.spots.getOrNull(index + 1)
    val target = if (next == null) 1f else progressBetween(state.totalSteps, here.steps, next.steps)
    val progress by animateFloatAsState(target, tween(800), label = "road")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bar = MaterialTheme.colorScheme.secondary

    SectionCard("たびの道") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${here.emoji} ${here.name}")
            Text(if (next != null) "${next.name} ❔" else "🏁 ゴール")
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(48.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * 0.8f
                val w = 6.dp.toPx()
                drawLine(
                    track, Offset(0f, y), Offset(size.width, y), w, StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
                )
                drawLine(bar, Offset(0f, y), Offset(size.width * progress, y), w, StrokeCap.Round)
            }
            val walker = 32.dp
            Text(
                "🚶",
                fontSize = 24.sp,
                modifier = Modifier
                    .offset(x = (maxWidth - walker) * progress)
                    .size(walker),
            )
        }
        Text(
            if (next != null) "つぎの場所まであと ${fmt(next.steps - state.totalSteps)} 歩"
            else "すべての場所をおとずれました！",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun JourneyScreen(state: GameState) {
    val reached = state.spotIndex + 1
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("たびの記録", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("おとずれた場所 $reached / ${Journey.spots.size}")
        }
        itemsIndexed(Journey.spots) { i, spot ->
            val unlocked = i <= state.spotIndex
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (unlocked) spot.emoji else "❔", fontSize = 34.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(if (unlocked) spot.name else "？？？", fontWeight = FontWeight.Bold)
                        Text(
                            if (unlocked) spot.description
                            else "あと ${fmt(spot.steps - state.totalSteps)} 歩",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text("${fmt(spot.steps)} 歩地点", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivalDialog(spot: Spot, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("やったー！") } },
        title = { Text("新しい場所にとうちゃく！") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(spot.emoji, fontSize = 64.sp)
                Text(spot.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(spot.description, textAlign = TextAlign.Center)
            }
        },
    )
}
