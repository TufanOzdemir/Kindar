package com.tufanozdemir.kindar.domain

import com.tufanozdemir.kindar.domain.ReciprocityEngine.DAY_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciprocityEngineTest {

    private val now = 1_800_000_000_000L

    private fun ev(kind: EventKind, points: Int, daysAgo: Int) =
        LedgerEvent(kind, points, now - daysAgo * DAY_MILLIS)

    private fun analyze(vararg events: LedgerEvent, type: RelationType = RelationType.FRIEND, importance: Int = 3) =
        ReciprocityEngine.analyze(events.toList(), type, importance, now)

    @Test
    fun `no events starts nice with two thirds trust`() {
        val a = analyze()
        assertEquals(Stance.NEW, a.stance)
        assertEquals(2.0 / 3.0, a.trust, 1e-9)
        assertEquals(0, a.net)
    }

    @Test
    fun `ledger sign convention - giving makes me creditor, receiving makes me debtor`() {
        val a = analyze(ev(EventKind.I_GAVE, 5, 3), ev(EventKind.THEY_GAVE, 2, 2))
        assertEquals(5, a.credit)
        assertEquals(2, a.debt)
        assertEquals(3, a.net)
    }

    @Test
    fun `their let down adds to my credit, my let down adds to my debt`() {
        val a = analyze(ev(EventKind.THEY_LET_DOWN, 4, 200), ev(EventKind.I_LET_DOWN, 1, 200))
        assertEquals(4, a.credit)
        assertEquals(1, a.debt)
    }

    @Test
    fun `received favour above threshold means it is my move`() {
        val a = analyze(ev(EventKind.THEY_GAVE, 5, 5))
        assertEquals(Stance.OWE, a.stance)
        assertTrue(a.advice.contains("5 puan"))
    }

    @Test
    fun `close relationships tolerate a bigger debt before nagging`() {
        val a = analyze(ev(EventKind.THEY_GAVE, 5, 5), type = RelationType.CLOSE)
        assertEquals(Stance.HEALTHY, a.stance)
    }

    @Test
    fun `single let down is forgiven - lenient not guard`() {
        val a = analyze(
            ev(EventKind.THEY_GAVE, 3, 40),
            ev(EventKind.I_GAVE, 3, 30),
            ev(EventKind.THEY_LET_DOWN, 8, 2),
        )
        assertEquals(Stance.LENIENT, a.stance)
    }

    @Test
    fun `repeated let downs trigger measured retaliation`() {
        val a = analyze(
            ev(EventKind.I_GAVE, 3, 50),
            ev(EventKind.THEY_LET_DOWN, 5, 20),
            ev(EventKind.THEY_LET_DOWN, 5, 5),
        )
        assertEquals(Stance.GUARD, a.stance)
        assertTrue(a.trust < 0.6)
    }

    @Test
    fun `old let downs fade - forgiveness through decay`() {
        val fresh = analyze(ev(EventKind.THEY_LET_DOWN, 6, 1), ev(EventKind.THEY_LET_DOWN, 6, 2))
        val old = analyze(ev(EventKind.THEY_LET_DOWN, 6, 720), ev(EventKind.THEY_LET_DOWN, 6, 721))
        assertTrue(old.trust > fresh.trust)
        assertTrue(old.stance != Stance.GUARD)
    }

    @Test
    fun `own unrepaired let down has top priority`() {
        val a = analyze(ev(EventKind.THEY_GAVE, 2, 30), ev(EventKind.I_LET_DOWN, 4, 3))
        assertEquals(Stance.REPAIR, a.stance)
    }

    @Test
    fun `repair is cleared after compensating`() {
        val a = analyze(ev(EventKind.I_LET_DOWN, 4, 10), ev(EventKind.I_GAVE, 4, 2))
        assertTrue(a.stance != Stance.REPAIR)
    }

    @Test
    fun `generosity within one third is healthy for friends`() {
        // 9 verdim, 6 aldım => %67 karşılık: GTFT sınırında, sağlıklı.
        val a = analyze(ev(EventKind.I_GAVE, 9, 10), ev(EventKind.THEY_GAVE, 6, 9))
        assertEquals(Stance.HEALTHY, a.stance)
    }

    @Test
    fun `beyond generosity tolerance is flagged one sided`() {
        val a = analyze(ev(EventKind.I_GAVE, 6, 20), ev(EventKind.I_GAVE, 6, 10), ev(EventKind.THEY_GAVE, 2, 5))
        assertEquals(Stance.ONE_SIDED, a.stance)
    }

    @Test
    fun `important dormant relation is surfaced, unimportant one is not`() {
        val important = analyze(ev(EventKind.I_GAVE, 2, 120), ev(EventKind.THEY_GAVE, 2, 119), importance = 5)
        val minor = analyze(ev(EventKind.I_GAVE, 2, 120), ev(EventKind.THEY_GAVE, 2, 119), importance = 1)
        assertEquals(Stance.DORMANT, important.stance)
        assertTrue(important.priority > 0)
        assertEquals(0.0, minor.priority, 0.0)
    }

    @Test
    fun `importance scales priority`() {
        val low = analyze(ev(EventKind.THEY_GAVE, 5, 1), importance = 1)
        val high = analyze(ev(EventKind.THEY_GAVE, 5, 1), importance = 5)
        assertTrue(high.priority > low.priority)
    }

    @Test
    fun `health is bounded`() {
        val a = analyze(ev(EventKind.THEY_LET_DOWN, 10, 1), ev(EventKind.THEY_LET_DOWN, 10, 2), ev(EventKind.THEY_LET_DOWN, 10, 3))
        assertTrue(a.health in 0..100)
        val b = analyze(ev(EventKind.THEY_GAVE, 10, 1), ev(EventKind.I_GAVE, 10, 1))
        assertTrue(b.health in 0..100)
        assertTrue(b.health > a.health)
    }

    @Test
    fun `self report classifies a taker`() {
        val events = listOf(ev(EventKind.THEY_GAVE, 8, 10), ev(EventKind.I_GAVE, 2, 5))
        val a = ReciprocityEngine.analyze(events, RelationType.FRIEND, 3, now)
        val r = ReciprocityEngine.selfReport(listOf(events to a), listOf(3), now)
        assertEquals(GiverStyle.TAKER_RISK, r.giverStyle)
        assertEquals(1, r.openDebts)
        assertEquals(6, r.totalDebt)
    }

    @Test
    fun `self report is unknown without data`() {
        val r = ReciprocityEngine.selfReport(emptyList(), emptyList(), now)
        assertEquals(GiverStyle.UNKNOWN, r.giverStyle)
        assertNull(r.givingRatio)
        assertTrue(r.tips.isNotEmpty())
    }
}
