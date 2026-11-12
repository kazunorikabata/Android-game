package com.kabata.eigo

import kotlin.random.Random

/** 盤面に置かれた単語 */
data class Entry(
    val q: Question,
    val row: Int,
    val col: Int,
    val across: Boolean,
    val number: Int,
) {
    val word: String get() = q.answer
    val cells: List<Pair<Int, Int>>
        get() = word.indices.map { i -> if (across) row to col + i else row + i to col }
}

/** できあがったクロスワード */
class Crossword(val rows: Int, val cols: Int, val entries: List<Entry>) {
    val solution: Map<Pair<Int, Int>, Char> = buildMap {
        entries.forEach { e -> e.cells.forEachIndexed { i, cell -> put(cell, e.word[i]) } }
    }
    val numbers: Map<Pair<Int, Int>, Int> = entries.associate { (it.row to it.col) to it.number }

    fun entriesAt(cell: Pair<Int, Int>): List<Int> = entries.indices.filter { cell in entries[it].cells }
}

/**
 * 単語リストからクロスワードを自動で組み立てる。
 * 交差が多くなるように置き、何回か試して一番よい配置を選ぶ。
 */
object CrosswordGenerator {
    private data class Draft(val q: Question, val row: Int, val col: Int, val across: Boolean)

    fun generate(questions: List<Question>, seed: Long, attempts: Int = 60): Crossword {
        val rng = Random(seed)
        val unique = questions.distinctBy { it.answer }
        val limit = maxOf(11, unique.maxOfOrNull { it.answer.length } ?: 0)
        var best: List<Draft> = emptyList()
        var bestScore = Int.MIN_VALUE
        repeat(attempts) {
            val drafts = attempt(unique, rng, limit)
            val score = drafts.size * 1000 - area(drafts)
            if (score > bestScore) {
                bestScore = score
                best = drafts
            }
        }
        return finish(best)
    }

    private fun attempt(questions: List<Question>, rng: Random, limit: Int): List<Draft> {
        val sorted = questions.shuffled(rng).sortedByDescending { it.answer.length + rng.nextInt(3) }
        val grid = HashMap<Pair<Int, Int>, Char>()
        // そのマスにヨコ(1)・タテ(2)のどちらの単語が通っているか
        val dirs = HashMap<Pair<Int, Int>, Int>()
        val placed = mutableListOf<Draft>()

        fun put(d: Draft) {
            d.q.answer.forEachIndexed { i, ch ->
                val cell = if (d.across) d.row to d.col + i else d.row + i to d.col
                grid[cell] = ch
                dirs[cell] = (dirs[cell] ?: 0) or (if (d.across) 1 else 2)
            }
            placed += d
        }

        fun tryPlace(q: Question): Boolean {
            val word = q.answer
            if (placed.isEmpty()) {
                put(Draft(q, 0, 0, rng.nextBoolean()))
                return true
            }
            var bestD: Draft? = null
            var bestInter = 0
            var ties = 0
            for ((cell, ch) in grid.entries.toList()) {
                val mask = dirs[cell] ?: 0
                for (i in word.indices) {
                    if (word[i] != ch) continue
                    for (across in listOf(true, false)) {
                        if (across && mask and 1 != 0) continue
                        if (!across && mask and 2 != 0) continue
                        val r = if (across) cell.first else cell.first - i
                        val c = if (across) cell.second - i else cell.second
                        val inter = score(word, r, c, across, grid, dirs, limit)
                        if (inter <= 0) continue
                        if (inter > bestInter) {
                            bestInter = inter; bestD = Draft(q, r, c, across); ties = 1
                        } else if (inter == bestInter) {
                            ties++
                            if (rng.nextInt(ties) == 0) bestD = Draft(q, r, c, across)
                        }
                    }
                }
            }
            bestD?.let { put(it); return true }
            return false
        }

        val left = sorted.filterNot { tryPlace(it) }
        // 置けなかった単語は、ほかの単語が増えたあとでもう一度試す
        left.forEach { tryPlace(it) }
        return placed
    }

    /** 置けるなら交差の数、置けないなら -1 */
    private fun score(
        word: String, r: Int, c: Int, across: Boolean,
        grid: Map<Pair<Int, Int>, Char>, dirs: Map<Pair<Int, Int>, Int>, limit: Int,
    ): Int {
        val dr = if (across) 0 else 1
        val dc = if (across) 1 else 0
        if (grid.containsKey(r - dr to c - dc)) return -1
        if (grid.containsKey(r + dr * word.length to c + dc * word.length)) return -1
        var inter = 0
        for (i in word.indices) {
            val cell = r + dr * i to c + dc * i
            val ch = grid[cell]
            if (ch != null) {
                if (ch != word[i]) return -1
                val mask = dirs[cell] ?: 0
                if (mask and (if (across) 1 else 2) != 0) return -1
                inter++
            } else {
                // となりのマスに文字があると、意図しない単語ができてしまう
                if (grid.containsKey(cell.first + dc to cell.second + dr)) return -1
                if (grid.containsKey(cell.first - dc to cell.second - dr)) return -1
            }
        }
        // 盤面が大きくなりすぎないように
        val rowsAll = grid.keys.map { it.first } + listOf(r, r + dr * (word.length - 1))
        val colsAll = grid.keys.map { it.second } + listOf(c, c + dc * (word.length - 1))
        if (rowsAll.max() - rowsAll.min() + 1 > limit) return -1
        if (colsAll.max() - colsAll.min() + 1 > limit) return -1
        return inter
    }

    private fun area(drafts: List<Draft>): Int {
        if (drafts.isEmpty()) return 0
        val cells = drafts.flatMap { d -> d.q.answer.indices.map { i -> if (d.across) d.row to d.col + i else d.row + i to d.col } }
        return (cells.maxOf { it.first } - cells.minOf { it.first } + 1) *
            (cells.maxOf { it.second } - cells.minOf { it.second } + 1)
    }

    /** 左上を (0,0) にそろえて、番号をふる */
    private fun finish(drafts: List<Draft>): Crossword {
        if (drafts.isEmpty()) return Crossword(0, 0, emptyList())
        val cells = drafts.flatMap { d -> d.q.answer.indices.map { i -> if (d.across) d.row to d.col + i else d.row + i to d.col } }
        val minR = cells.minOf { it.first }
        val minC = cells.minOf { it.second }
        val rows = cells.maxOf { it.first } - minR + 1
        val cols = cells.maxOf { it.second } - minC + 1
        val starts = drafts.map { (it.row - minR) to (it.col - minC) }.distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
        val numberOf = starts.withIndex().associate { (i, cell) -> cell to i + 1 }
        val entries = drafts.map { d ->
            val r = d.row - minR
            val c = d.col - minC
            Entry(d.q, r, c, d.across, numberOf.getValue(r to c))
        }.sortedWith(compareBy({ !it.across }, { it.number }))
        return Crossword(rows, cols, entries)
    }
}
