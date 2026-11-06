package com.kabata.sanpo

import android.content.Context
import java.time.LocalDate

data class GameState(
    val totalSteps: Long = 0,
    val todaySteps: Long = 0,
    val dailyGoal: Long = 5_000,
    val seenSpotIndex: Int = 0,
    val goalDays: Int = 0,
) {
    val spotIndex: Int get() = Journey.spotIndexFor(totalSteps)
    val hasNewArrival: Boolean get() = spotIndex > seenSpotIndex
}

/**
 * 歩数とゲーム進行を端末に保存する。
 *
 * TYPE_STEP_COUNTER は「端末起動からの累計歩数」を返すので、前回の値との差分を
 * ゲームの歩数として加算する。アプリを閉じている間に歩いた分も、次に開いたときに反映される。
 */
class StepStore(context: Context) {
    private val prefs = context.getSharedPreferences("sanpo", Context.MODE_PRIVATE)

    @Synchronized
    fun load(): GameState {
        rollDay()
        return read()
    }

    @Synchronized
    fun onSensorValue(value: Long): GameState {
        val last = prefs.getLong(K_LAST_SENSOR, -1)
        val delta = when {
            last < 0 -> 0L          // 初回：基準値として記録するだけ
            value < last -> value   // 端末が再起動されてカウンタがリセットされた
            else -> value - last
        }
        prefs.edit().putLong(K_LAST_SENSOR, value).apply()
        return addSteps(delta)
    }

    @Synchronized
    fun addSteps(delta: Long): GameState {
        rollDay()
        if (delta <= 0) return read()
        val before = read()
        val today = before.todaySteps + delta
        val edit = prefs.edit()
            .putLong(K_TOTAL, before.totalSteps + delta)
            .putLong(K_TODAY, today)
        if (before.todaySteps < before.dailyGoal && today >= before.dailyGoal &&
            prefs.getString(K_GOAL_DATE, null) != todayKey()
        ) {
            edit.putInt(K_GOAL_DAYS, before.goalDays + 1).putString(K_GOAL_DATE, todayKey())
        }
        edit.apply()
        return read()
    }

    @Synchronized
    fun markSpotSeen(index: Int): GameState {
        prefs.edit().putInt(K_SEEN_SPOT, index).apply()
        return read()
    }

    private fun read() = GameState(
        totalSteps = prefs.getLong(K_TOTAL, 0),
        todaySteps = prefs.getLong(K_TODAY, 0),
        seenSpotIndex = prefs.getInt(K_SEEN_SPOT, 0),
        goalDays = prefs.getInt(K_GOAL_DAYS, 0),
    )

    private fun rollDay() {
        if (prefs.getString(K_DAY, null) != todayKey()) {
            prefs.edit().putString(K_DAY, todayKey()).putLong(K_TODAY, 0).apply()
        }
    }

    private fun todayKey() = LocalDate.now().toString()

    private companion object {
        const val K_LAST_SENSOR = "last_sensor"
        const val K_TOTAL = "total"
        const val K_TODAY = "today"
        const val K_DAY = "day"
        const val K_SEEN_SPOT = "seen_spot"
        const val K_GOAL_DAYS = "goal_days"
        const val K_GOAL_DATE = "goal_date"
    }
}
