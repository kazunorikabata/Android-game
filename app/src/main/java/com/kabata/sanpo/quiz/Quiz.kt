package com.kabata.sanpo.quiz

import android.content.Context
import android.location.Location
import com.kabata.sanpo.data.Castle
import com.kabata.sanpo.data.Castles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 1問ぶん。choices はシャッフル済み、answer は正解の位置 */
data class QuizQuestion(val text: String, val choices: List<String>, val answer: Int)

private data class RawQuestion(val no: Int, val text: String, val correct: String, val wrong: List<String>)

object Quiz {
    const val MAX_QUESTIONS = 10

    private val raw: Map<Int, List<RawQuestion>> by lazy {
        (QUIZ_EAST + QUIZ_CENTRAL + QUIZ_WEST).lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val p = line.split('|').map { it.trim() }
                val no = p.getOrNull(0)?.toIntOrNull()
                if (p.size != 6 || no == null) null else RawQuestion(no, p[1], p[2], p.subList(3, 6))
            }
            .groupBy { it.no }
    }

    /** そのお城の問題数（手書きの問題＋自動で作る問題） */
    fun countFor(no: Int): Int = minOf(MAX_QUESTIONS, (raw[no]?.size ?: 0) + autoQuestions(Castles.byNo(no)).size)

    /** 出題する問題を作る（毎回順番と選択肢が変わる） */
    fun build(no: Int): List<QuizQuestion> {
        val castle = Castles.byNo(no)
        val all = (raw[no].orEmpty() + autoQuestions(castle)).shuffled().take(MAX_QUESTIONS)
        return all.map { q ->
            val choices = (q.wrong + q.correct).shuffled()
            QuizQuestion(q.text, choices, choices.indexOf(q.correct))
        }
    }

    /** お城のデータから正確に作れる問題（都道府県・番号・いちばん近いお城） */
    private fun autoQuestions(castle: Castle): List<RawQuestion> {
        val list = mutableListOf<RawQuestion>()
        val region = Castles.regionOf(castle.no)

        // 都道府県（同じ地方の県をハズレにして少し難しく）
        val samePrefs = region.range.map { Castles.byNo(it).pref }.distinct().filter { it != castle.pref }
        val otherPrefs = Castles.all.map { it.pref }.distinct().filter { it != castle.pref && it !in samePrefs }
        val prefChoices = (samePrefs.shuffled() + otherPrefs.shuffled()).take(3)
        list += RawQuestion(castle.no, "${castle.name}がある都道府県は？", castle.pref, prefChoices)

        // 100名城の番号
        val nums = (1..100).filter { it != castle.no && kotlin.math.abs(it - castle.no) <= 6 }.shuffled().take(3)
        list += RawQuestion(castle.no, "${castle.name}は日本100名城の何番？", "${castle.no}番", nums.map { "${it}番" })

        // いちばん近い100名城（2位と差が小さいときは出題しない）
        val others = Castles.all.filter { it.no != castle.no }
            .map { it to distance(castle, it) }
            .sortedBy { it.second }
        val (nearest, d1) = others[0]
        val d2 = others[1].second
        if (d2 > d1 * 1.3f) {
            val wrong = others.drop(1).filter { it.second > d1 * 1.5f }
                .take(12).shuffled().take(3).map { it.first.name }
            if (wrong.size == 3) {
                list += RawQuestion(castle.no, "${castle.name}からいちばん近い100名城は？", nearest.name, wrong)
            }
        }
        return list
    }

    private fun distance(a: Castle, b: Castle): Float {
        val out = FloatArray(1)
        Location.distanceBetween(a.lat, a.lon, b.lat, b.lon, out)
        return out[0]
    }
}

/** クイズの最高記録（端末内に保存） */
object QuizRepo {
    data class Best(val score: Int, val total: Int) {
        val perfect: Boolean get() = total > 0 && score >= total
    }

    private val _best = MutableStateFlow<Map<Int, Best>>(emptyMap())
    val best: StateFlow<Map<Int, Best>> = _best.asStateFlow()

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("quiz", Context.MODE_PRIVATE)
        prefs = p
        _best.value = (1..100).mapNotNull { no ->
            val total = p.getInt("total_$no", 0)
            if (total == 0) null else no to Best(p.getInt("score_$no", 0), total)
        }.toMap()
    }

    /** 結果を記録する。自己ベストを更新したら true */
    fun record(no: Int, score: Int, total: Int): Boolean {
        val old = _best.value[no]
        val better = old == null || score.toFloat() / total > old.score.toFloat() / old.total
        if (better) {
            prefs?.edit()?.putInt("score_$no", score)?.putInt("total_$no", total)?.apply()
            _best.value = _best.value + (no to Best(score, total))
        }
        return better
    }
}
