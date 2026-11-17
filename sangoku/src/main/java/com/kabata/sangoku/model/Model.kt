package com.kabata.sangoku.model

import com.kabata.sangoku.data.CITIES
import com.kabata.sangoku.data.FACTIONS
import com.kabata.sangoku.data.GENERALS
import com.kabata.sangoku.data.ROADS
import org.json.JSONArray
import org.json.JSONObject

class City(val id: Int, val name: String, val x: Float, val y: Float) {
    var owner = -1
    var troops = 0
    var gold = 0
    var food = 0
    var agri = 0
    var comm = 0
    var defense = 0
    val neighbors = mutableListOf<Int>()
}

class General(val id: Int, val name: String, val war: Int, val intel: Int, val pol: Int) {
    /** 所属勢力（-1 は在野） */
    var faction = -1
    var city = 0
    var acted = false
    val total: Int get() = war + intel + pol
}

class Faction(val id: Int, val name: String, val color: Long, val difficulty: String, val desc: String) {
    var ruler = -1
}

/** ゲーム全体の状態 */
class Game(val player: Int) {
    val cities = mutableListOf<City>()
    val generals = mutableListOf<General>()
    val factions = mutableListOf<Faction>()
    var year = 196
    var month = 1

    fun cityByName(name: String) = cities.first { it.name == name }
    fun citiesOf(f: Int) = cities.filter { it.owner == f }
    fun generalsIn(city: City) = generals.filter { it.city == city.id && it.faction == city.owner && it.faction >= 0 }
    fun freeIn(city: City) = generals.filter { it.city == city.id && it.faction == -1 }
    fun generalsOf(f: Int) = generals.filter { it.faction == f }
    fun alive(f: Int) = cities.any { it.owner == f }
    fun factionName(f: Int) = if (f < 0) "空白地" else factions[f].name + "軍"

    companion object {
        /** 最初の状態をつくる */
        fun newGame(player: Int): Game {
            val g = Game(player)
            g.setupStatic()
            CITIES.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
                val p = line.split('|')
                val c = g.cities[i]
                c.owner = g.factions.indexOfFirst { it.name == p[3] }
                c.troops = p[4].toInt(); c.gold = p[5].toInt(); c.food = p[6].toInt()
                c.agri = p[7].toInt(); c.comm = p[8].toInt(); c.defense = p[9].toInt()
            }
            GENERALS.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
                val p = line.split('|')
                val gen = g.generals[i]
                gen.faction = g.factions.indexOfFirst { it.name == p[4] }
                gen.city = g.cityByName(p[5]).id
            }
            g.factions.forEach { f -> f.ruler = g.generals.first { it.name == f.name }.id }
            return g
        }

        fun fromJson(text: String): Game {
            val o = JSONObject(text)
            val g = Game(o.getInt("player"))
            g.setupStatic()
            g.year = o.getInt("year")
            g.month = o.getInt("month")
            val cs = o.getJSONArray("cities")
            for (i in 0 until cs.length()) {
                val a = cs.getJSONArray(i)
                val c = g.cities[i]
                c.owner = a.getInt(0); c.troops = a.getInt(1); c.gold = a.getInt(2); c.food = a.getInt(3)
                c.agri = a.getInt(4); c.comm = a.getInt(5); c.defense = a.getInt(6)
            }
            val gs = o.getJSONArray("generals")
            for (i in 0 until gs.length()) {
                val a = gs.getJSONArray(i)
                val gen = g.generals[i]
                gen.faction = a.getInt(0); gen.city = a.getInt(1); gen.acted = a.getBoolean(2)
            }
            val fs = o.getJSONArray("rulers")
            for (i in 0 until fs.length()) g.factions[i].ruler = fs.getInt(i)
            return g
        }
    }

    /** 変わらないデータ（都市・道・武将の能力・勢力）を読みこむ */
    private fun setupStatic() {
        CITIES.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
            val p = line.split('|')
            cities += City(i, p[0], p[1].toFloat(), p[2].toFloat())
        }
        ROADS.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val (a, b) = line.split('-')
            val ca = cityByName(a)
            val cb = cityByName(b)
            ca.neighbors += cb.id
            cb.neighbors += ca.id
        }
        FACTIONS.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
            val p = line.split('|')
            factions += Faction(i, p[0], p[1].toLong(16), p[2], p[3])
        }
        GENERALS.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { i, line ->
            val p = line.split('|')
            generals += General(i, p[0], p[1].toInt(), p[2].toInt(), p[3].toInt())
        }
    }

    fun toJson(): String {
        val o = JSONObject()
        o.put("player", player).put("year", year).put("month", month)
        val cs = JSONArray()
        cities.forEach { c ->
            cs.put(JSONArray().put(c.owner).put(c.troops).put(c.gold).put(c.food).put(c.agri).put(c.comm).put(c.defense))
        }
        o.put("cities", cs)
        val gs = JSONArray()
        generals.forEach { gen -> gs.put(JSONArray().put(gen.faction).put(gen.city).put(gen.acted)) }
        o.put("generals", gs)
        val fs = JSONArray()
        factions.forEach { fs.put(it.ruler) }
        o.put("rulers", fs)
        return o.toString()
    }
}
