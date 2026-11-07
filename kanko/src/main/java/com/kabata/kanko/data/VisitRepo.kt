package com.kabata.kanko.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** 訪問の記録 */
data class Visit(
    val spotId: String,
    val time: Long,
    val photo: String,
    /** true: 現地で撮影 / false: 過去の写真から登録 */
    val onSite: Boolean,
)

/** 訪問記録の保存（端末内の JSON ファイル） */
object VisitRepo {
    data class State(val visits: List<Visit> = emptyList()) {
        val visitedIds: Set<String> = visits.mapTo(HashSet()) { it.spotId }
        val count: Int get() = visitedIds.size
        val visitedPrefs: Set<String> = visitedIds.mapNotNullTo(HashSet()) { Spots.byId(it)?.pref }
        fun visitsFor(id: String) = visits.filter { it.spotId == id }
        fun firstVisit(id: String) = visits.filter { it.spotId == id }.minByOrNull { it.time }
        fun countIn(pref: String) = Spots.inPref(pref).count { it.id in visitedIds }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var file: File? = null

    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.filesDir, "visits.json")
        file = f
        _state.value = State(runCatching { parse(f.readText()) }.getOrDefault(emptyList()))
    }

    fun newPhotoFile(context: Context): File {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        return File(dir, "spot_${System.currentTimeMillis()}.jpg")
    }

    @Synchronized
    fun addVisit(spotId: String, photo: File, onSite: Boolean): Visit {
        val v = Visit(spotId, System.currentTimeMillis(), photo.absolutePath, onSite)
        save(_state.value.visits + v)
        return v
    }

    @Synchronized
    fun deleteVisit(visit: Visit) {
        File(visit.photo).delete()
        save(_state.value.visits.filterNot { it.photo == visit.photo })
    }

    private fun save(visits: List<Visit>) {
        val arr = JSONArray()
        visits.forEach {
            arr.put(
                JSONObject().put("spotId", it.spotId).put("time", it.time)
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
            Visit(o.getString("spotId"), o.getLong("time"), o.getString("photo"), o.optBoolean("onSite", true))
        }
    }
}
