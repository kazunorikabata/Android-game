package com.kabata.kanko.data

import android.location.Location

data class Spot(
    /** 保存用のID（都道府県＋名前） */
    val id: String,
    val pref: String,
    val name: String,
    val emoji: String,
    val lat: Double,
    val lon: Double,
    val desc: String,
    val radius: Int,
)

data class Region(val name: String, val prefs: List<String>)

data class Rank(val name: String, val count: Int)

object Spots {
    val regions = listOf(
        Region("北海道・東北", listOf("北海道", "青森県", "岩手県", "宮城県", "秋田県", "山形県", "福島県")),
        Region("関東", listOf("茨城県", "栃木県", "群馬県", "埼玉県", "千葉県", "東京都", "神奈川県")),
        Region("中部", listOf("新潟県", "富山県", "石川県", "福井県", "山梨県", "長野県", "岐阜県", "静岡県", "愛知県")),
        Region("近畿", listOf("三重県", "滋賀県", "京都府", "大阪府", "兵庫県", "奈良県", "和歌山県")),
        Region("中国", listOf("鳥取県", "島根県", "岡山県", "広島県", "山口県")),
        Region("四国", listOf("徳島県", "香川県", "愛媛県", "高知県")),
        Region("九州・沖縄", listOf("福岡県", "佐賀県", "長崎県", "熊本県", "大分県", "宮崎県", "鹿児島県", "沖縄県")),
    )

    val prefectures: List<String> = regions.flatMap { it.prefs }

    val all: List<Spot> by lazy {
        (SPOTS_EAST + SPOTS_CENTRAL + SPOTS_WEST).lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val p = line.split('|').map { it.trim() }
                if (p.size < 6) return@mapNotNull null
                val lat = p[3].toDoubleOrNull() ?: return@mapNotNull null
                val lon = p[4].toDoubleOrNull() ?: return@mapNotNull null
                Spot(
                    id = "${p[0]}:${p[1]}", pref = p[0], name = p[1], emoji = p[2],
                    lat = lat, lon = lon, desc = p[5],
                    radius = p.getOrNull(6)?.toIntOrNull() ?: 1000,
                )
            }
            .toList()
    }

    private val byId: Map<String, Spot> by lazy { all.associateBy { it.id } }

    fun byId(id: String): Spot? = byId[id]

    fun inPref(pref: String): List<Spot> = all.filter { it.pref == pref }

    fun regionOf(pref: String): Region = regions.first { pref in it.prefs }

    val ranks = listOf(
        Rank("観光ビギナー", 0),
        Rank("たびびと", 1),
        Rank("旅好き", 10),
        Rank("旅の達人", 30),
        Rank("観光マスター", 100),
        Rank("日本通", 250),
        Rank("伝説の旅人", 480),
    )

    fun rankIndexFor(count: Int): Int = ranks.indexOfLast { count >= it.count }.coerceAtLeast(0)

    fun distanceTo(spot: Spot, loc: Location): Float {
        val out = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, spot.lat, spot.lon, out)
        return out[0]
    }

    /** 現地でチェックインできる距離にいるか（GPS の誤差も少し考慮） */
    fun isNear(spot: Spot, loc: Location?): Boolean {
        loc ?: return false
        return distanceTo(spot, loc) <= spot.radius + loc.accuracy.coerceAtMost(100f)
    }

    /** 現在地にいちばん近いスポットの都道府県 */
    fun nearestPref(loc: Location): String = all.minBy { distanceTo(it, loc) }.pref
}
