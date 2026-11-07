package com.kabata.sanpo.meisho

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId

/** 名所（OpenStreetMap のデータから取得） */
data class Meisho(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val emoji: String,
    val category: String,
    val points: Int,
    /** チェックインできる距離（m）。公園など広い場所は大きめ */
    val radius: Int,
)

/** 名所で撮った写真の記録 */
data class Visit(
    val spotId: String,
    val name: String,
    val emoji: String,
    val points: Int,
    val time: Long,
    val photo: String,
)

/**
 * 名所めぐりモード。
 * まわりの名所を OpenStreetMap（Overpass API）から探し、その場所で写真を撮るとポイントがもらえる。
 * - はじめての名所：名所ごとのポイント
 * - 別の日にまた来た：+5 ポイント
 * - 同じ日に何枚撮っても、ポイントは1日1回
 */
object MeishoRepo {
    const val REVISIT_POINTS = 5
    const val SEARCH_RADIUS_M = 1500

    data class State(
        val spots: List<Meisho> = emptyList(),
        val loading: Boolean = false,
        val error: String? = null,
        val visits: List<Visit> = emptyList(),
    ) {
        val totalPoints: Int get() = visits.sumOf { it.points }
        val visitedIds: Set<String> get() = visits.mapTo(HashSet()) { it.spotId }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var initialized = false
    private lateinit var dir: File

    fun photoDir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        dir = File(context.filesDir, "meisho").apply { mkdirs() }
        val visits = runCatching { parseVisits(File(dir, "visits.json").readText()) }.getOrDefault(emptyList())
        val spots = runCatching { parseSpots(File(dir, "spots.json").readText()) }.getOrDefault(emptyList())
        _state.value = State(spots = spots, visits = visits)
    }

    fun spot(id: String): Meisho? = _state.value.spots.firstOrNull { it.id == id }

    fun distanceTo(spot: Meisho, loc: Location): Float {
        val out = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, spot.lat, spot.lon, out)
        return out[0]
    }

    /** このあと写真を撮ったら何ポイントもらえるか */
    fun pointsFor(spot: Meisho, now: Long = System.currentTimeMillis()): Int {
        val mine = _state.value.visits.filter { it.spotId == spot.id }
        return when {
            mine.isEmpty() -> spot.points
            mine.any { sameDay(it.time, now) } -> 0
            else -> REVISIT_POINTS
        }
    }

    @Synchronized
    fun checkIn(spot: Meisho, photo: File): Visit {
        val now = System.currentTimeMillis()
        val visit = Visit(spot.id, spot.name, spot.emoji, pointsFor(spot, now), now, photo.absolutePath)
        val visits = _state.value.visits + visit
        File(dir, "visits.json").writeText(visitsToJson(visits))
        _state.value = _state.value.copy(visits = visits)
        return visit
    }

    suspend fun search(lat: Double, lon: Double) {
        _state.value = _state.value.copy(loading = true, error = null)
        try {
            val body = withContext(Dispatchers.IO) { fetch(lat, lon) }
            val spots = parseOverpass(body)
                .sortedBy { s ->
                    val out = FloatArray(1)
                    Location.distanceBetween(lat, lon, s.lat, s.lon, out)
                    out[0]
                }
            withContext(Dispatchers.IO) { File(dir, "spots.json").writeText(spotsToJson(spots)) }
            _state.value = _state.value.copy(spots = spots, loading = false)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                loading = false,
                error = "名所を探せませんでした。電波のよい場所でもう一度ためしてください。",
            )
        }
    }

    private fun fetch(lat: Double, lon: Double): String {
        val around = "(around:$SEARCH_RADIUS_M,$lat,$lon)"
        val query = """
            [out:json][timeout:25];
            (
              nwr["tourism"~"^(attraction|viewpoint|museum|gallery|zoo|aquarium|theme_park|artwork)$"]["name"]$around;
              nwr["historic"]["name"]$around;
              nwr["amenity"="place_of_worship"]["name"]$around;
              nwr["leisure"~"^(park|garden)$"]["name"]$around;
              node["natural"~"^(peak|waterfall)$"]["name"]$around;
            );
            out center 150;
        """.trimIndent()
        val conn = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 40_000
            conn.setRequestProperty("User-Agent", "TekutekuSanpo/0.3 (Android)")
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseOverpass(body: String): List<Meisho> {
        val elements = JSONObject(body).optJSONArray("elements") ?: return emptyList()
        val result = LinkedHashMap<String, Meisho>()
        for (i in 0 until elements.length()) {
            val e = elements.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: continue
            val name = tags.optString("name:ja").ifBlank { tags.optString("name") }
            if (name.isBlank()) continue
            val center = e.optJSONObject("center")
            val lat = if (e.has("lat")) e.getDouble("lat") else center?.optDouble("lat") ?: continue
            val lon = if (e.has("lon")) e.getDouble("lon") else center?.optDouble("lon") ?: continue
            val type = e.optString("type")
            val kind = classify(tags) ?: continue
            val id = "${type}_${e.optLong("id")}"
            // 同じ名前の名所が近くに重複していることがあるので、名前でまとめる
            if (result.values.any { it.name == name }) continue
            result[id] = Meisho(
                id = id, name = name, lat = lat, lon = lon,
                emoji = kind.emoji, category = kind.label, points = kind.points,
                radius = if (type == "node") 100 else 250,
            )
        }
        return result.values.toList()
    }

    private data class Kind(val emoji: String, val label: String, val points: Int)

    private fun classify(tags: JSONObject): Kind? {
        val tourism = tags.optString("tourism")
        val historic = tags.optString("historic")
        val leisure = tags.optString("leisure")
        val natural = tags.optString("natural")
        return when {
            natural == "peak" -> Kind("⛰️", "山", 40)
            natural == "waterfall" -> Kind("💧", "滝", 30)
            tourism == "viewpoint" -> Kind("🌄", "展望スポット", 30)
            historic == "castle" -> Kind("🏯", "お城", 40)
            historic.isNotEmpty() && historic != "yes" -> Kind("📜", "史跡", 30)
            tags.optString("amenity") == "place_of_worship" -> when (tags.optString("religion")) {
                "shinto" -> Kind("⛩️", "神社", 20)
                "buddhist" -> Kind("🙏", "お寺", 20)
                "christian" -> Kind("⛪", "教会", 20)
                else -> Kind("🙏", "祈りの場所", 15)
            }
            tourism == "museum" || tourism == "gallery" -> Kind("🏛️", "博物館・美術館", 20)
            tourism == "zoo" || tourism == "aquarium" -> Kind("🐧", "動物園・水族館", 25)
            tourism == "theme_park" -> Kind("🎡", "テーマパーク", 25)
            tourism == "attraction" -> Kind("⭐", "観光スポット", 25)
            tourism == "artwork" -> Kind("🗿", "アート", 10)
            leisure == "garden" -> Kind("🌸", "庭園", 20)
            leisure == "park" -> Kind("🌳", "公園", 10)
            else -> null
        }
    }

    private fun sameDay(a: Long, b: Long): Boolean {
        val z = ZoneId.systemDefault()
        return Instant.ofEpochMilli(a).atZone(z).toLocalDate() == Instant.ofEpochMilli(b).atZone(z).toLocalDate()
    }

    private fun spotsToJson(spots: List<Meisho>) = JSONArray().apply {
        spots.forEach {
            put(
                JSONObject().put("id", it.id).put("name", it.name).put("lat", it.lat).put("lon", it.lon)
                    .put("emoji", it.emoji).put("category", it.category).put("points", it.points)
                    .put("radius", it.radius),
            )
        }
    }.toString()

    private fun parseSpots(text: String): List<Meisho> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Meisho(
                o.getString("id"), o.getString("name"), o.getDouble("lat"), o.getDouble("lon"),
                o.getString("emoji"), o.getString("category"), o.getInt("points"), o.getInt("radius"),
            )
        }
    }

    private fun visitsToJson(visits: List<Visit>) = JSONArray().apply {
        visits.forEach {
            put(
                JSONObject().put("spotId", it.spotId).put("name", it.name).put("emoji", it.emoji)
                    .put("points", it.points).put("time", it.time).put("photo", it.photo),
            )
        }
    }.toString()

    private fun parseVisits(text: String): List<Visit> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Visit(
                o.getString("spotId"), o.getString("name"), o.getString("emoji"),
                o.getInt("points"), o.getLong("time"), o.getString("photo"),
            )
        }
    }
}
