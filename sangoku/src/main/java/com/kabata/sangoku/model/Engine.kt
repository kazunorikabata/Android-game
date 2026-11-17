package com.kabata.sangoku.model

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class Develop(val label: String, val cost: Int) {
    AGRI("開墾", 50), COMM("商業", 50), WALL("築城", 80)
}

/** 合戦の結果 */
class BattleResult(
    val lines: List<String>,
    val won: Boolean,
    val city: City,
    /** 攻め側がつかまえた武将（城を落としたとき） */
    val captured: List<General>,
)

const val DRAFT_COST_PER_1000 = 150

fun formatTroops(n: Int): String = if (n >= 10000) "%.1f万".format(n / 10000.0) else "$n"

/** ゲームのルール */
object Engine {
    private val rng = Random(System.currentTimeMillis())

    // ---------- 内政 ----------

    fun develop(g: Game, gen: General, city: City, kind: Develop): String {
        if (city.gold < kind.cost) return "金が足りません（必要 ${kind.cost}）"
        city.gold -= kind.cost
        gen.acted = true
        return when (kind) {
            Develop.AGRI -> {
                val up = 10 + gen.pol / 4 + rng.nextInt(6)
                city.agri = min(999, city.agri + up)
                "${gen.name}が開墾しました。農業 +$up"
            }
            Develop.COMM -> {
                val up = 10 + gen.pol / 4 + rng.nextInt(6)
                city.comm = min(999, city.comm + up)
                "${gen.name}が商業を発展させました。商業 +$up"
            }
            Develop.WALL -> {
                val up = 8 + gen.pol / 6 + gen.war / 10 + rng.nextInt(6)
                city.defense = min(999, city.defense + up)
                "${gen.name}が城壁を修理しました。防御 +$up"
            }
        }
    }

    fun draftAmount(gen: General) = 1000 + gen.pol * 10 + gen.war * 5

    fun draftCost(gen: General) = draftAmount(gen) * DRAFT_COST_PER_1000 / 1000

    fun draft(g: Game, gen: General, city: City): String {
        val amount = draftAmount(gen)
        val cost = draftCost(gen)
        if (city.gold < cost) return "金が足りません（必要 $cost）"
        city.gold -= cost
        city.troops += amount
        gen.acted = true
        return "${gen.name}が兵を集めました。兵 +$amount"
    }

    fun recruitChance(recruiter: General, target: General): Int =
        (45 + recruiter.intel / 2 + recruiter.pol / 5 - target.intel / 4 - target.war / 8).coerceIn(10, 90)

    /** 在野・捕虜の武将を仲間にさそう */
    fun recruit(g: Game, recruiter: General, target: General, faction: Int, city: City): Pair<Boolean, String> {
        recruiter.acted = true
        val ok = rng.nextInt(100) < recruitChance(recruiter, target)
        return if (ok) {
            target.faction = faction
            target.city = city.id
            target.acted = true
            true to "${target.name}「${recruiter.name}殿のお誘いとあらば、喜んでお仕えいたします」"
        } else {
            false to "${target.name}「せっかくですが、お断りいたします」"
        }
    }

    // ---------- 軍事 ----------

    fun move(g: Game, gens: List<General>, from: City, to: City, troops: Int): String {
        val t = troops.coerceIn(0, from.troops)
        from.troops -= t
        to.troops += t
        gens.forEach { it.city = to.id; it.acted = true }
        return "${gens.joinToString("・") { it.name }}が${to.name}へ移動しました（兵 ${formatTroops(t)}）"
    }

    fun battle(g: Game, gens: List<General>, from: City, target: City, troopsIn: Int): BattleResult {
        val lines = mutableListOf<String>()
        val attacker = from.owner
        val defender = target.owner
        var a = troopsIn.coerceIn(0, from.troops)
        from.troops -= a
        gens.forEach { it.acted = true }
        var d = target.troops
        val defGens = g.generalsIn(target).sortedByDescending { it.war }

        lines += "${g.factionName(attacker)}（${gens.joinToString("・") { it.name }}・兵${formatTroops(a)}）が" +
            "${target.name}（${g.factionName(defender)}・兵${formatTroops(d)}）に攻めこんだ！"

        val aLead = gens.maxOfOrNull { it.war } ?: 30
        val aSmart = gens.maxOfOrNull { it.intel } ?: 30
        val dLead = defGens.maxOfOrNull { it.war } ?: 40
        val dSmart = defGens.maxOfOrNull { it.intel } ?: 40

        // 一騎討ち
        if (gens.isNotEmpty() && defGens.isNotEmpty() && rng.nextInt(100) < 45) {
            val x = gens.maxBy { it.war }
            val y = defGens.maxBy { it.war }
            if (x.war >= 70 && y.war >= 70) {
                lines += "⚔ ${x.name}と${y.name}の一騎討ち！"
                val xs = x.war + rng.nextInt(30)
                val ys = y.war + rng.nextInt(30)
                if (xs >= ys) {
                    lines += "　${x.name}が${y.name}を打ち負かした！ 守備兵の士気が下がった"
                    d -= d * 15 / 100
                } else {
                    lines += "　${y.name}が${x.name}を退けた！ 攻撃軍の士気が下がった"
                    a -= a * 15 / 100
                }
            }
        }
        // 計略
        if (gens.isNotEmpty() && rng.nextInt(100) < (aSmart - dSmart + 30).coerceIn(5, 70)) {
            val who = gens.maxBy { it.intel }
            val dmg = d * (10 + rng.nextInt(8)) / 100
            d -= dmg
            lines += "🔥 ${who.name}の火計が成功！ 守備兵 -${formatTroops(dmg)}"
        }
        if (defGens.isNotEmpty() && rng.nextInt(100) < (dSmart - aSmart + 20).coerceIn(5, 60)) {
            val who = defGens.maxBy { it.intel }
            val dmg = a * (8 + rng.nextInt(8)) / 100
            a -= dmg
            lines += "🏹 ${who.name}の伏兵が攻撃軍をおそった！ 攻撃軍 -${formatTroops(dmg)}"
        }

        // 本戦
        val wallBonus = 1.0 + target.defense / 700.0
        var round = 0
        while (round < 8 && a > 0 && d > 0) {
            round++
            val aPow = a * (0.6 + aLead / 250.0 + aSmart / 600.0)
            val dPow = d * (0.6 + dLead / 250.0 + dSmart / 600.0) * wallBonus
            val dLoss = max(80, (aPow * 0.13 * (0.8 + rng.nextDouble() * 0.4)).toInt())
            val aLoss = max(80, (dPow * 0.13 * (0.8 + rng.nextDouble() * 0.4)).toInt())
            d = max(0, d - dLoss)
            a = max(0, a - aLoss)
            // 守備側が大きく崩れたら城を明け渡す
            if (d > 0 && a > 0 && d < a / 6) d = 0
        }
        val won = d <= 0 && a > 0
        val captured = mutableListOf<General>()
        if (won) {
            lines += "🏯 ${target.name}を攻め落とした！（残り兵 ${formatTroops(a)}）"
            // 守っていた武将は逃げるか、つかまる
            defGens.forEach { gen ->
                val escape = target.neighbors.map { g.cities[it] }.filter { it.owner == defender && defender >= 0 }
                if (escape.isNotEmpty() && rng.nextInt(100) < 50) {
                    val to = escape.random(rng)
                    gen.city = to.id
                    lines += "　${gen.name}は${to.name}へ逃げのびた"
                } else {
                    captured += gen
                    gen.faction = -1
                    gen.city = target.id
                    lines += "　${gen.name}をとらえた！"
                }
            }
            target.owner = attacker
            target.troops = a
            target.gold /= 2
            target.food /= 2
            target.defense = max(100, target.defense * 3 / 4)
            gens.forEach { it.city = target.id }
            fixRuler(g, defender)
        } else {
            lines += "🛡 ${target.name}は守りきった。攻撃軍は${from.name}へ退却（残り兵 ${formatTroops(a)}）"
            target.troops = d
            from.troops += a
        }
        return BattleResult(lines, won, target, captured)
    }

    /** 君主がいなくなった勢力に新しい君主を立てる */
    private fun fixRuler(g: Game, f: Int) {
        if (f < 0) return
        val faction = g.factions[f]
        if (g.generals[faction.ruler].faction == f) return
        val next = g.generalsOf(f).maxByOrNull { it.total } ?: return
        faction.ruler = next.id
    }

    // ---------- ターン進行 ----------

    /** プレイヤーがターンを終えたあとの処理。出来事のリストを返す */
    fun endTurn(g: Game): List<String> {
        val report = mutableListOf<String>()
        for (f in g.factions.indices.shuffled(rng)) {
            if (f == g.player || !g.alive(f)) continue
            runAi(g, f, report)
        }
        monthEnd(g, report)
        return report
    }

    private fun monthEnd(g: Game, report: MutableList<String>) {
        g.cities.forEach { c ->
            if (c.owner >= 0) {
                c.gold += c.comm / 2 + 20
                c.food += c.agri / 3 + 20
                c.food -= c.troops / 100
                if (c.food < 0) {
                    val run = c.troops / 10
                    c.troops -= run
                    c.food = 0
                    if (c.owner == g.player) report += "⚠ ${c.name}で兵糧がつき、兵 ${formatTroops(run)} が逃げ出した"
                }
            } else {
                // 空白地は少しずつ兵が戻る
                if (c.troops < 12000) c.troops += 150
            }
        }
        g.generals.forEach { it.acted = false }
        g.month++
        if (g.month > 12) {
            g.month = 1
            g.year++
        }
        // ほろびた勢力
        g.factions.forEach { f ->
            if (!g.alive(f.id) && g.generalsOf(f.id).isNotEmpty()) {
                g.generalsOf(f.id).forEach { it.faction = -1 }
                report += "💀 ${f.name}軍は滅亡した"
            }
        }
    }

    /** コンピューターの勢力の行動 */
    private fun runAi(g: Game, f: Int, report: MutableList<String>) {
        var attacked = false
        for (city in g.citiesOf(f).shuffled(rng)) {
            if (city.owner != f) continue
            val enemies = city.neighbors.map { g.cities[it] }.filter { it.owner != f }
            // 攻撃（1ターンに1回まで）
            if (!attacked && enemies.isNotEmpty() && city.troops >= 7000 && rng.nextInt(100) < 55) {
                val target = enemies.minByOrNull { it.troops * (1 + it.defense / 700.0) }!!
                val strength = target.troops * (1 + target.defense / 700.0) * 1.1
                val send = city.troops * 7 / 10
                val gens = g.generalsIn(city).filter { !it.acted }.sortedByDescending { it.war }.take(2)
                if (send > strength && gens.isNotEmpty()) {
                    val involvesPlayer = target.owner == g.player
                    val r = battle(g, gens, city, target, send)
                    attacked = true
                    if (involvesPlayer || r.won) {
                        report += r.lines.first()
                        report += r.lines.last { it.startsWith("🏯") || it.startsWith("🛡") }
                    }
                    // AIがつかまえた武将：仲間にするか、逃がす
                    r.captured.forEach { cap ->
                        if (rng.nextInt(100) < 35) {
                            cap.faction = f
                            cap.city = r.city.id
                            if (involvesPlayer) report += "　${cap.name}は${g.factionName(f)}に降った"
                        }
                    }
                }
            }
            if (city.owner != f) continue
            // 在野武将の登用
            val free = g.freeIn(city)
            val recruiter = g.generalsIn(city).filter { !it.acted }.maxByOrNull { it.intel }
            if (free.isNotEmpty() && recruiter != null && rng.nextInt(100) < 50) {
                val target = free.random(rng)
                val (ok, _) = recruit(g, recruiter, target, f, city)
                if (ok) report += "📜 ${target.name}が${g.factionName(f)}に仕えた"
            }
            // 内政
            val frontline = enemies.isNotEmpty()
            for (gen in g.generalsIn(city).filter { !it.acted }) {
                when {
                    frontline && city.troops < 9000 + city.defense * 8 && city.gold >= draftCost(gen) -> draft(g, gen, city)
                    city.agri < 650 && city.gold >= 50 && rng.nextBoolean() -> develop(g, gen, city, Develop.AGRI)
                    city.comm < 650 && city.gold >= 50 -> develop(g, gen, city, Develop.COMM)
                    frontline && city.defense < 500 && city.gold >= 80 -> develop(g, gen, city, Develop.WALL)
                    city.gold >= draftCost(gen) + 200 -> draft(g, gen, city)
                }
            }
            // 後方の兵を前線へ送る
            if (!frontline && city.troops > 9000) {
                val front = city.neighbors.map { g.cities[it] }.filter { n ->
                    n.owner == f && n.neighbors.any { g.cities[it].owner != f }
                }
                if (front.isNotEmpty()) {
                    val to = front.random(rng)
                    val gens = g.generalsIn(city).sortedByDescending { it.war }.take(1)
                    move(g, gens, city, to, city.troops / 2)
                }
            }
        }
    }

    fun checkEnd(g: Game): String? = when {
        g.cities.all { it.owner == g.player } -> "win"
        g.citiesOf(g.player).isEmpty() -> "lose"
        else -> null
    }
}
