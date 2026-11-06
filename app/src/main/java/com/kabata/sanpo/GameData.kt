package com.kabata.sanpo

/** 旅の途中で訪れる場所 */
data class Spot(val name: String, val emoji: String, val steps: Long, val description: String)

/** 相棒「テクトリ」の成長段階 */
data class PetStage(val name: String, val emoji: String, val steps: Long)

object Journey {
    val spots = listOf(
        Spot("おうち", "🏠", 0, "ここから旅がはじまる。いってきます！"),
        Spot("ちかくの公園", "🌳", 500, "ベンチでひとやすみ。ハトがあいさつしてくれた。"),
        Spot("パン屋さん", "🥐", 1_500, "焼きたての香りにつられて寄り道。"),
        Spot("川ぞいの道", "🌊", 3_000, "水面がきらきら。カモがのんびり泳いでいる。"),
        Spot("ちいさな神社", "⛩️", 5_000, "おまいりして、旅の安全をおねがいした。"),
        Spot("ひまわり畑", "🌻", 8_000, "背の高いひまわりが道を見守っている。"),
        Spot("丘の上の展望台", "🔭", 12_000, "町がぜんぶ見わたせる！"),
        Spot("海辺", "🏖️", 18_000, "潮風がきもちいい。貝がらをひろった。"),
        Spot("港町", "⚓", 25_000, "大きな船が汽笛を鳴らしている。"),
        Spot("森の温泉", "♨️", 35_000, "歩きつかれた足にしみわたる…。"),
        Spot("山のてっぺん", "🏔️", 50_000, "雲の上まで来てしまった！"),
        Spot("星ふる村", "🌌", 70_000, "夜空いっぱいの星。願いごとをひとつ。"),
        Spot("虹のはし", "🌈", 100_000, "伝説の場所にたどりついた。おめでとう！"),
    )

    fun spotIndexFor(totalSteps: Long): Int =
        spots.indexOfLast { totalSteps >= it.steps }.coerceAtLeast(0)
}

object Pet {
    const val NAME = "テクトリ"

    val stages = listOf(
        PetStage("たまご", "🥚", 0),
        PetStage("ひな", "🐣", 2_000),
        PetStage("こども", "🐤", 10_000),
        PetStage("おとな", "🐦", 30_000),
        PetStage("そらの鳥", "🕊️", 80_000),
    )

    fun stageIndexFor(totalSteps: Long): Int =
        stages.indexOfLast { totalSteps >= it.steps }.coerceAtLeast(0)
}

/** 2地点間の進み具合（0〜1） */
fun progressBetween(total: Long, from: Long, to: Long): Float =
    if (to <= from) 1f else ((total - from).toFloat() / (to - from)).coerceIn(0f, 1f)
