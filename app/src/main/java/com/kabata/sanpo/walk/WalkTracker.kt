package com.kabata.sanpo.walk

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.osmdroid.util.GeoPoint
import java.io.File
import java.time.LocalDate
import kotlin.math.floor

/**
 * GPS のお散歩記録。サービスと画面の両方から使うシングルトン。
 *
 * - 地図を約 50m 四方の「マス」に区切り、歩いた場所のまわり 3x3 マスの霧を晴らす
 * - 今日のルートと歩いた距離を記録する
 * - データはすべて端末内（アプリ専用領域）にだけ保存する
 */
object WalkTracker {
    /** 1マスの大きさ（度）。緯度方向で約 55m */
    const val CELL = 0.0005

    /** これより精度が悪い測位は捨てる（m） */
    private const val MAX_ACCURACY_M = 35f

    /** これより速い移動は乗り物とみなして記録しない（m/秒 ≒ 25km/h） */
    private const val MAX_WALK_SPEED = 7.0

    data class State(
        val recording: Boolean = false,
        val segments: List<List<GeoPoint>> = emptyList(),
        val cells: Set<Long> = emptySet(),
        val todayMeters: Double = 0.0,
        val totalMeters: Double = 0.0,
        val lastPoint: GeoPoint? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var initialized = false
    private lateinit var dir: File
    private lateinit var prefs: android.content.SharedPreferences
    private var lastFix: Location? = null
    private val cells = HashSet<Long>()

    fun cellKey(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
    fun cellX(key: Long): Int = (key shr 32).toInt()
    fun cellY(key: Long): Int = key.toInt()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        dir = File(context.filesDir, "walk").apply { mkdirs() }
        prefs = context.getSharedPreferences("walk", Context.MODE_PRIVATE)
        cellsFile().takeIf { it.exists() }?.forEachLine { line -> line.toLongOrNull()?.let(cells::add) }
        rollDay()
        val segments = loadRoute()
        val last = segments.lastOrNull { it.isNotEmpty() }?.last()
            ?: if (prefs.contains("last_lat")) {
                GeoPoint(prefs.getFloat("last_lat", 0f).toDouble(), prefs.getFloat("last_lon", 0f).toDouble())
            } else null
        _state.value = State(
            segments = segments,
            cells = HashSet(cells),
            todayMeters = prefs.getFloat("today_m", 0f).toDouble(),
            totalMeters = prefs.getFloat("total_m", 0f).toDouble(),
            lastPoint = last,
        )
    }

    @Synchronized
    fun startSession() {
        lastFix = null
        rollDay()
        routeFile().appendText("-\n")
        _state.value = _state.value.copy(recording = true, segments = _state.value.segments + listOf(emptyList()))
    }

    @Synchronized
    fun stopSession() {
        lastFix = null
        _state.value = _state.value.copy(recording = false)
    }

    @Synchronized
    fun addFix(loc: Location) {
        if (!initialized || !_state.value.recording) return
        if (loc.hasAccuracy() && loc.accuracy > MAX_ACCURACY_M) return
        if (rollDay()) {
            _state.value = _state.value.copy(segments = listOf(emptyList()), todayMeters = 0.0)
            routeFile().appendText("-\n")
        }

        var s = _state.value
        val prev = lastFix
        var added = 0.0
        if (prev != null) {
            val d = prev.distanceTo(loc).toDouble()
            val dt = (loc.time - prev.time) / 1000.0
            if (d < 4) return // GPS のゆらぎ
            if (dt > 0 && d / dt > MAX_WALK_SPEED) {
                // 乗り物での移動：ルートを区切って、距離・ぬりつぶしには数えない
                lastFix = loc
                routeFile().appendText("-\n")
                _state.value = s.copy(segments = s.segments + listOf(emptyList()))
                return
            }
            added = d
        }
        lastFix = loc

        val point = GeoPoint(loc.latitude, loc.longitude)
        routeFile().appendText("${loc.latitude},${loc.longitude}\n")
        val segments = if (s.segments.isEmpty()) listOf(listOf(point))
        else s.segments.dropLast(1) + listOf(s.segments.last() + point)

        // まわり 3x3 マスの霧を晴らす
        val cx = floor(loc.longitude / CELL).toInt()
        val cy = floor(loc.latitude / CELL).toInt()
        val newKeys = StringBuilder()
        for (dx in -1..1) for (dy in -1..1) {
            val key = cellKey(cx + dx, cy + dy)
            if (cells.add(key)) newKeys.append(key).append('\n')
        }
        if (newKeys.isNotEmpty()) cellsFile().appendText(newKeys.toString())

        val today = s.todayMeters + added
        val total = s.totalMeters + added
        prefs.edit()
            .putFloat("today_m", today.toFloat())
            .putFloat("total_m", total.toFloat())
            .putFloat("last_lat", loc.latitude.toFloat())
            .putFloat("last_lon", loc.longitude.toFloat())
            .apply()

        s = s.copy(
            segments = segments,
            cells = if (newKeys.isNotEmpty()) HashSet(cells) else s.cells,
            todayMeters = today,
            totalMeters = total,
            lastPoint = point,
        )
        _state.value = s
    }

    /** 日付が変わっていたら今日の記録をリセットする。変わったら true */
    private fun rollDay(): Boolean {
        val today = LocalDate.now().toString()
        if (prefs.getString("day", null) == today) return false
        prefs.edit().putString("day", today).putFloat("today_m", 0f).apply()
        return true
    }

    private fun loadRoute(): List<List<GeoPoint>> {
        val file = routeFile()
        if (!file.exists()) return emptyList()
        val result = mutableListOf<MutableList<GeoPoint>>()
        file.forEachLine { line ->
            if (line == "-") {
                result.add(mutableListOf())
            } else {
                val parts = line.split(',')
                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                val lon = parts.getOrNull(1)?.toDoubleOrNull()
                if (lat != null && lon != null) {
                    if (result.isEmpty()) result.add(mutableListOf())
                    result.last().add(GeoPoint(lat, lon))
                }
            }
        }
        return result.filter { it.isNotEmpty() }
    }

    private fun cellsFile() = File(dir, "cells.txt")
    private fun routeFile() = File(dir, "route_${LocalDate.now()}.txt")
}
