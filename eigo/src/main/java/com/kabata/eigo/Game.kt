package com.kabata.eigo

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Question(val level: Int, val index: Int, val sentence: String, val answer: String, val ja: String) {
    val key: String get() = "$level:$index"
}

data class Level(val no: Int, val name: String, val desc: String, val decoys: Int)

object Game {
    const val STAGE_SIZE = 10

    val levels = listOf(
        Level(1, "⭐ はじめて", "中1レベルの基本単語", 0),
        Level(2, "⭐⭐ 中学", "中2〜中3の文法・熟語", 1),
        Level(3, "⭐⭐⭐ 高校", "高校レベルの単語・熟語", 2),
        Level(4, "👑 チャレンジ", "大学受験・TOEICレベル", 3),
    )

    val questions: List<Question> by lazy {
        val counters = HashMap<Int, Int>()
        QUESTIONS.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { line ->
            val p = line.split('|')
            val level = p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            if (p.size != 4) return@mapNotNull null
            val i = counters.getOrDefault(level, 0)
            counters[level] = i + 1
            Question(level, i, p[1], p[2].lowercase(), p[3])
        }.toList()
    }

    private val byKey by lazy { questions.associateBy { it.key } }

    fun inLevel(level: Int) = questions.filter { it.level == level }

    fun stageCount(level: Int) = (inLevel(level).size + STAGE_SIZE - 1) / STAGE_SIZE

    fun stage(level: Int, stage: Int): List<Question> =
        inLevel(level).drop(stage * STAGE_SIZE).take(STAGE_SIZE)

    fun byKey(key: String): Question? = byKey[key]

    fun decoysFor(level: Int): Int = levels.firstOrNull { it.no == level }?.decoys ?: 0

    /** ペナルティ（ミス・ヒント・パス）から星の数を決める */
    fun starsFor(penalty: Int): Int = when {
        penalty == 0 -> 3
        penalty <= 3 -> 2
        else -> 1
    }
}

/** 星の記録とまちがえた問題（端末内に保存） */
object Progress {
    data class State(val stars: Map<String, Int> = emptyMap(), val missed: Set<String> = emptySet()) {
        fun starsOf(level: Int, stage: Int) = stars["$level-$stage"] ?: 0
        val totalStars: Int get() = stars.values.sum()
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
        prefs = p
        val stars = p.all.filterKeys { it.startsWith("stars_") }
            .mapNotNull { (k, v) -> (v as? Int)?.let { k.removePrefix("stars_") to it } }.toMap()
        _state.value = State(stars, p.getStringSet("missed", emptySet())!!.toSet())
    }

    fun isUnlocked(level: Int, stage: Int): Boolean =
        stage == 0 || _state.value.starsOf(level, stage - 1) > 0

    fun recordStage(level: Int, stage: Int, stars: Int): Boolean {
        val key = "$level-$stage"
        val old = _state.value.stars[key] ?: 0
        if (stars <= old) return false
        prefs?.edit()?.putInt("stars_$key", stars)?.apply()
        _state.value = _state.value.copy(stars = _state.value.stars + (key to stars))
        return true
    }

    fun markMissed(q: Question) = updateMissed(_state.value.missed + q.key)

    fun markLearned(q: Question) = updateMissed(_state.value.missed - q.key)

    private fun updateMissed(missed: Set<String>) {
        prefs?.edit()?.putStringSet("missed", missed)?.apply()
        _state.value = _state.value.copy(missed = missed)
    }
}
