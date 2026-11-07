package com.kabata.sanpo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kabata.sanpo.data.Castles
import com.kabata.sanpo.quiz.Quiz
import com.kabata.sanpo.quiz.QuizRepo

private val CorrectColor = Color(0xFF2E7D32)

@Composable
fun QuizDialog(no: Int, onClose: () -> Unit) {
    val castle = Castles.byNo(no)
    var round by remember { mutableIntStateOf(0) }
    val questions = remember(no, round) { Quiz.build(no) }
    var index by remember(round) { mutableIntStateOf(0) }
    var selected by remember(round) { mutableStateOf<Int?>(null) }
    var score by remember(round) { mutableIntStateOf(0) }
    var finished by remember(round) { mutableStateOf(false) }
    var newBest by remember(round) { mutableStateOf(false) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("No.${castle.no} ${castle.name}", style = MaterialTheme.typography.labelLarge)
                        Text("お城クイズ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onClose) { Text("✕ とじる") }
                }

                if (!finished) {
                    val q = questions[index]
                    LinearProgressIndicator(
                        progress = { (index + 1f) / questions.size },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("第 ${index + 1} 問 / ${questions.size}　（正解 $score）", style = MaterialTheme.typography.bodySmall)
                    Text(q.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))

                    q.choices.forEachIndexed { i, choice ->
                        val answered = selected != null
                        val colors = when {
                            !answered -> ButtonDefaults.outlinedButtonColors()
                            i == q.answer -> ButtonDefaults.outlinedButtonColors(
                                containerColor = CorrectColor, contentColor = Color.White,
                            )
                            i == selected -> ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                            else -> ButtonDefaults.outlinedButtonColors()
                        }
                        OutlinedButton(
                            onClick = {
                                if (selected == null) {
                                    selected = i
                                    if (i == q.answer) score++
                                }
                            },
                            colors = colors,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(choice, fontSize = 16.sp, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    }

                    selected?.let { s ->
                        Text(
                            if (s == q.answer) "⭕ 正解！" else "❌ ざんねん… 正解は「${q.choices[q.answer]}」",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (s == q.answer) CorrectColor else MaterialTheme.colorScheme.error,
                        )
                        Button(
                            onClick = {
                                if (index + 1 < questions.size) {
                                    index++
                                    selected = null
                                } else {
                                    finished = true
                                    newBest = QuizRepo.record(no, score, questions.size)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (index + 1 < questions.size) "つぎの問題へ" else "結果を見る") }
                    }
                } else {
                    val total = questions.size
                    val perfect = score == total
                    Spacer(Modifier.height(24.dp))
                    Text(
                        if (perfect) "🎓" else if (score * 2 >= total) "👍" else "📖",
                        fontSize = 72.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "$score / $total 問 正解",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        when {
                            perfect -> "満点！${castle.name}はかせに認定！"
                            score * 2 >= total -> "なかなかの城通です！"
                            else -> "お城のことをもっと知るともっと楽しくなります"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    if (newBest) {
                        Text(
                            "自己ベスト更新！",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { round++ }, modifier = Modifier.fillMaxWidth()) { Text("もう一度ちょうせん") }
                    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("とじる") }
                }
            }
        }
    }
}
