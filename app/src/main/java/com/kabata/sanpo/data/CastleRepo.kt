package com.kabata.sanpo.data

import android.content.Context
import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 登城の記録 */
data class Visit(
    val no: Int,
    val time: Long,
    val photo: String,
    /** true: 現地で撮影 / false: 過去の写真から登録 */
    val onSite: Boolean,
)

/** 登城記録の保存（端末内の JSON ファイル） */
object CastleRepo {
    data class State(val visits: List<Visit> = emptyList()) {
        val visitedNos: Set<Int> = visits.mapTo(HashSet()) { it.no }
        val count: Int get() = visitedNos.size
        fun visitsFor(no: Int) = visits.filter { it.no == no }
        fun firstVisit(no: Int) = visits.filter { it.no == no }.minByOrNull { it.time }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "visits.json")
        file = f
        val visits = runCatching { parse(f.readText()) }.getOrDefault(emptyList())
        _state.value = State(visits)
    }

    fun newPhotoFile(context: Context, no: Int): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "castle${no}_${System.currentTimeMillis()}.jpg")
    }

    @Synchronized
    fun addVisit(no: Int, photo: File, onSite: Boolean): Visit {
        val v = Visit(no, System.currentTimeMillis(), photo.absolutePath, onSite)
        save(_state.value.visits + v)
        return v
    }

    @Synchronized
    fun deleteVisit(visit: Visit) {
        File(visit.photo).delete()
        save(_state.value.visits.filterNot { it.photo == visit.photo })
    }

    fun distanceTo(castle: Castle, loc: Location): Float {
        val out = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, castle.lat, castle.lon, out)
        return out[0]
    }

    /** 現地で登城できる距離にいるか（GPS の誤差も少し考慮） */
    fun isNear(castle: Castle, loc: Location?): Boolean {
        loc ?: return false
        return distanceTo(castle, loc) <= castle.radius + loc.accuracy.coerceAtMost(100f)
    }

    private fun save(visits: List<Visit>) {
        val arr = JSONArray()
        visits.forEach {
            arr.put(
                JSONObject().put("no", it.no).put("time", it.time)
                    .put("photo", it.photo).put("onSite", it.onSite),
            )
        }
        file?.writeText(arr.toString())
        _state.value = State(visits)
    }

    private fun parse(text: String): List<Visit> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Visit(o.getInt("no"), o.getLong("time"), o.getString("photo"), o.optBoolean("onSite", true))
        }
    }
}
