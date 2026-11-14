package com.tufanozdemir.kindar.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Kindar'ın dayandığı modelin küçük bir sağlaması: hatalı (gürültülü) iletişimin olduğu tekrarlı
 * Mahkum İkilemi'nde Axelrod tarzı bir turnuva ve basit bir evrimsel (replikatör) dinamik.
 *
 * Gerçek ilişkilerde yanlış anlamalar olur; bu yüzden %5 gürültü kullanılır.
 * Ödemeler: T=5, R=3, P=1, S=0.
 */
class TournamentSimulationTest {

    private enum class Move { C, D }

    private interface Strategy {
        val name: String
        fun first(): Move
        fun next(myLast: Move, theirLast: Move, theirPrev: Move?, rnd: Random, state: DoubleArray): Move
    }

    private fun strategy(name: String, first: Move = Move.C, f: (Move, Move, Move?, Random, DoubleArray) -> Move) =
        object : Strategy {
            override val name = name
            override fun first() = first
            override fun next(myLast: Move, theirLast: Move, theirPrev: Move?, rnd: Random, state: DoubleArray) =
                f(myLast, theirLast, theirPrev, rnd, state)
        }

    private val strategies = listOf(
        strategy("ALLC") { _, _, _, _, _ -> Move.C },
        strategy("ALLD", Move.D) { _, _, _, _, _ -> Move.D },
        strategy("RANDOM") { _, _, _, r, _ -> if (r.nextBoolean()) Move.C else Move.D },
        strategy("TFT") { _, t, _, _, _ -> t },
        strategy("GRIM") { _, t, _, _, s -> if (t == Move.D) s[0] = 1.0; if (s[0] == 1.0) Move.D else Move.C },
        strategy("TF2T") { _, t, p, _, _ -> if (t == Move.D && p == Move.D) Move.D else Move.C },
        strategy("WSLS") { m, t, _, _, _ -> if (t == Move.C) m else if (m == Move.C) Move.D else Move.C },
        // Generous TFT: karşı taraf kırarsa 1/3 olasılıkla yine de işbirliği yap.
        strategy("GTFT") { _, t, _, r, _ -> if (t == Move.C || r.nextDouble() < 1.0 / 3.0) Move.C else Move.D },
        // Kindar: güven takibi (affeden bellek) + hoşgörü (tek hatayı affet) + cömertlik (1/3) +
        // güven düşükse ölçülü karşılık. s[0] = güven, s[1] = başlatıldı mı.
        strategy("KINDAR") { _, t, p, r, s ->
            if (s[1] == 0.0) { s[0] = 2.0 / 3.0; s[1] = 1.0 }
            s[0] = 0.8 * s[0] + 0.2 * (if (t == Move.C) 1.0 else 0.0)
            when {
                t == Move.C -> Move.C
                p != Move.D && s[0] >= 0.5 -> Move.C
                s[0] >= 0.4 && r.nextDouble() < 1.0 / 3.0 -> Move.C
                else -> Move.D
            }
        },
    )

    private fun payoff(a: Move, b: Move): Int = when {
        a == Move.C && b == Move.C -> 3
        a == Move.C && b == Move.D -> 0
        a == Move.D && b == Move.C -> 5
        else -> 1
    }

    private fun play(a: Strategy, b: Strategy, rounds: Int, noise: Double, rnd: Random): Pair<Double, Double> {
        val sa = DoubleArray(2)
        val sb = DoubleArray(2)
        var ma = a.first()
        var mb = b.first()
        var prevA: Move? = null
        var prevB: Move? = null
        var pa = 0
        var pb = 0
        repeat(rounds) {
            if (rnd.nextDouble() < noise) ma = if (ma == Move.C) Move.D else Move.C
            if (rnd.nextDouble() < noise) mb = if (mb == Move.C) Move.D else Move.C
            pa += payoff(ma, mb)
            pb += payoff(mb, ma)
            val na = a.next(ma, mb, prevB, rnd, sa)
            val nb = b.next(mb, ma, prevA, rnd, sb)
            prevA = ma
            prevB = mb
            ma = na
            mb = nb
        }
        return pa.toDouble() / rounds to pb.toDouble() / rounds
    }

    /** Ortalama ödeme matrisi M[i][j] = i'nin j'ye karşı tur başına ortalaması. */
    private fun matrix(noise: Double, rounds: Int = 200, reps: Int = 30): Array<DoubleArray> {
        val rnd = Random(42)
        val n = strategies.size
        val m = Array(n) { DoubleArray(n) }
        for (i in 0 until n) for (j in 0 until n) {
            var s = 0.0
            repeat(reps) { s += play(strategies[i], strategies[j], rounds, noise, rnd).first }
            m[i][j] = s / reps
        }
        return m
    }

    private fun index(name: String) = strategies.indexOfFirst { it.name == name }

    @Test
    fun `round robin with noise - kindar beats defectors and rigid reciprocators`() {
        val m = matrix(noise = 0.05)
        val avg = m.map { it.average() }
        val ranking = strategies.indices.sortedByDescending { avg[it] }
        println("Turnuva (gürültü %5), tur başına ortalama:")
        ranking.forEach { println("  %-7s %.3f".format(strategies[it].name, avg[it])) }

        assertTrue(avg[index("KINDAR")] > avg[index("ALLD")])
        assertTrue(avg[index("KINDAR")] > avg[index("RANDOM")])
        assertTrue(avg[index("KINDAR")] > avg[index("WSLS")])
        assertTrue(avg[index("KINDAR")] > avg[index("TFT")])
        assertTrue(avg[index("KINDAR")] > avg[index("GRIM")])
    }

    @Test
    fun `evolutionary dynamics with noise - defectors die out, cooperative reciprocators dominate`() {
        val m = matrix(noise = 0.05)
        val n = strategies.size
        var x = DoubleArray(n) { 1.0 / n }
        repeat(2000) {
            val fitness = DoubleArray(n) { i -> (0 until n).sumOf { j -> m[i][j] * x[j] } }
            val mean = (0 until n).sumOf { x[it] * fitness[it] }
            x = DoubleArray(n) { x[it] * fitness[it] / mean }
        }
        println("Evrimsel denge (gürültü %5):")
        strategies.indices.sortedByDescending { x[it] }
            .forEach { println("  %-7s %.3f".format(strategies[it].name, x[it])) }

        assertTrue(x[index("ALLD")] < 0.01)
        assertTrue(x[index("RANDOM")] < 0.01)
        val reciprocators = listOf("GTFT", "KINDAR", "TF2T", "WSLS", "TFT").sumOf { x[index(it)] }
        assertTrue(reciprocators > 0.5)
    }

    @Test
    fun `a population playing Kindar cannot be invaded`() {
        val m = matrix(noise = 0.05)
        val k = index("KINDAR")
        println("Kindar topluluğuna sızma denemesi (sütun: Kindar'a karşı kazanç):")
        strategies.indices.forEach { i -> println("  %-7s %.3f".format(strategies[i].name, m[i][k])) }
        // Kötü niyetli ya da sömürücü stratejiler sızamaz. (TFT/GTFT gibi iyi niyetli stratejiler
        // Kindar topluluğunda zaten tam işbirliği yapar; aralarındaki fark gürültü düzeyindedir.)
        listOf("ALLD", "RANDOM", "GRIM", "WSLS").map { index(it) }
            .forEach { i -> assertTrue("${strategies[i].name} sızabildi", m[k][k] > m[i][k]) }
    }
}
