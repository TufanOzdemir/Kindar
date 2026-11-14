package com.tufanozdemir.kindar.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Kindar'ın karar motoru: "Cömert Karşılıklılık" modeli.
 *
 * Tekrarlı Mahkum İkilemi literatüründe uzun vadede kazanan stratejilerin ortak özellikleri
 * (Axelrod 1984; Nowak & Sigmund 1992, 1993; Fudenberg, Rand & Dreber 2012; Stewart & Plotkin 2013;
 * Hilbe, Chatterjee & Nowak 2018) şu kurallara çevrilmiştir:
 *
 *  1. İYİ NİYETLİ BAŞLA  – Yeni ilişkide güven ön kabulü 2/3'tür; ilk adımı sen at ([Stance.NEW]).
 *  2. HOŞGÖRÜLÜ OL      – Tek bir kırgınlık kalıp sayılmaz ([Stance.LENIENT]); sınır koymak için
 *                          tekrar eden ihlal gerekir ([Stance.GUARD]).
 *  3. KARŞILIK VER       – Tekrar eden ihlale ölçülü karşılık verilir: ilişki kesilmez, yatırım azaltılır.
 *  4. AFFET              – Geçmiş olayların güvene etkisi yarı ömür ([TRUST_HALF_LIFE_DAYS]) ile söner.
 *  5. CÖMERT OL          – Karşılıksız kalan vermenin bir kısmı ilişki türüne göre tolere edilir
 *                          ([RelationType.generosityTolerance], arkadaşlıkta GTFT'nin 1/3'ü).
 *  6. HATANI ONAR        – Kendi ihlalin varsa ilk öncelik onarmaktır ([Stance.REPAIR]); WSLS'nin
 *                          "karşılıklı kırgınlıktan hızla çık" kuralı.
 *  7. BORCUNU ÖDE        – Aldığın iyiliğe karşılık vermek itibarını (dolaylı karşılıklılık) korur ([Stance.OWE]).
 *  8. GELECEĞİN GÖLGESİ  – Önemli ve uykuda kalan ilişkiler canlandırılır ([Stance.DORMANT]); işbirliği
 *                          ancak ilişkinin geleceği yeterince değerliyse sürer.
 */
object ReciprocityEngine {

    const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000

    /** Güvenin hesaplanmasında geçmiş olayların ağırlığı her 180 günde yarıya iner (affetme). */
    const val TRUST_HALF_LIFE_DAYS = 180.0

    /** Tek taraflılık için son dönem ağırlıklandırması. */
    const val BALANCE_HALF_LIFE_DAYS = 365.0

    /** Güven ön kabulü: 4 puan işbirliği, 2 puan ihlal => başlangıç güveni 2/3 ("nice"). */
    const val PRIOR_COOPERATION = 4.0
    const val PRIOR_DEFECTION = 2.0

    const val RECENT_WINDOW_DAYS = 90
    const val REPAIR_WINDOW_DAYS = 90

    /** Bu güvenin altı, en az iki ihlalle birlikte sınır koymayı gerektirir. */
    const val LOW_TRUST = 0.40

    /** Tek taraflılık uyarısı için verilmiş en az (ağırlıklı) puan. */
    const val ONE_SIDED_MIN_VOLUME = 8.0

    fun analyze(
        events: List<LedgerEvent>,
        relationType: RelationType,
        importance: Int,
        nowMillis: Long,
    ): RelationshipAnalysis {
        val imp = importance.coerceIn(1, 5)
        if (events.isEmpty()) {
            val priority = if (imp >= 4) Stance.NEW.basePriority * importanceFactor(imp) else 0.0
            return RelationshipAnalysis(
                credit = 0,
                debt = 0,
                net = 0,
                trust = PRIOR_COOPERATION / (PRIOR_COOPERATION + PRIOR_DEFECTION),
                health = 50,
                stance = Stance.NEW,
                advice = newAdvice(relationType),
                priority = priority,
                eventCount = 0,
                lastEventMillis = null,
                recentTheirLetDowns = 0,
            )
        }

        val sorted = events.sortedBy { it.occurredAtMillis }
        val pts = { e: LedgerEvent -> e.points.coerceIn(1, 10) }

        val credit = sorted.filter { it.kind.ledgerSign > 0 }.sumOf(pts)
        val debt = sorted.filter { it.kind.ledgerSign < 0 }.sumOf(pts)
        val net = credit - debt

        // --- Güven (karşı tarafın işbirliği eğilimi), affeden bellekle ---
        var theirCoop = 0.0
        var theirDefect = 0.0
        sorted.forEach { e ->
            val w = decay(nowMillis - e.occurredAtMillis, TRUST_HALF_LIFE_DAYS) * pts(e)
            when (e.kind) {
                EventKind.THEY_GAVE -> theirCoop += w
                EventKind.THEY_LET_DOWN -> theirDefect += w
                else -> Unit
            }
        }
        val trust = (theirCoop + PRIOR_COOPERATION) /
            (theirCoop + theirDefect + PRIOR_COOPERATION + PRIOR_DEFECTION)

        // --- Son dönem verme dengesi ---
        var myGivenRecent = 0.0
        var theirGivenRecent = 0.0
        sorted.forEach { e ->
            val w = decay(nowMillis - e.occurredAtMillis, BALANCE_HALF_LIFE_DAYS) * pts(e)
            when (e.kind) {
                EventKind.I_GAVE -> myGivenRecent += w
                EventKind.THEY_GAVE -> theirGivenRecent += w
                else -> Unit
            }
        }

        val recentCutoff = nowMillis - RECENT_WINDOW_DAYS * DAY_MILLIS
        val recentTheirLetDowns = sorted.count {
            it.kind == EventKind.THEY_LET_DOWN && it.occurredAtMillis >= recentCutoff
        }
        val totalTheirLetDowns = sorted.count { it.kind == EventKind.THEY_LET_DOWN }

        val last = sorted.last().occurredAtMillis
        val daysSinceLast = max(0.0, (nowMillis - last).toDouble() / DAY_MILLIS)

        // --- Onarılmamış kendi hatam ---
        val lastMyLetDown = sorted.lastOrNull { it.kind == EventKind.I_LET_DOWN }
        val unrepaired = lastMyLetDown != null &&
            lastMyLetDown.occurredAtMillis >= nowMillis - REPAIR_WINDOW_DAYS * DAY_MILLIS &&
            sorted.filter { it.kind == EventKind.I_GAVE && it.occurredAtMillis >= lastMyLetDown.occurredAtMillis }
                .sumOf(pts) < pts(lastMyLetDown)

        val tolerance = relationType.generosityTolerance
        val oweThreshold = if (relationType.communal) 6 else 3

        val guard = (recentTheirLetDowns >= 2 && trust < 0.60) ||
            (totalTheirLetDowns >= 2 && trust < LOW_TRUST)
        val oneSided = myGivenRecent >= ONE_SIDED_MIN_VOLUME &&
            theirGivenRecent < (1.0 - tolerance) * myGivenRecent &&
            net > 0
        val dormant = daysSinceLast > relationType.dormantAfterDays

        val stance = when {
            unrepaired -> Stance.REPAIR
            guard -> Stance.GUARD
            -net >= oweThreshold -> Stance.OWE
            oneSided -> Stance.ONE_SIDED
            recentTheirLetDowns >= 1 -> Stance.LENIENT
            dormant -> Stance.DORMANT
            else -> Stance.HEALTHY
        }

        // --- Sağlık skoru ---
        val volume = credit + debt
        val fairness = 1.0 - min(1.0, abs(net) / (0.5 * volume + 5.0))
        val recency = decay((nowMillis - last), relationType.dormantAfterDays.toDouble())
        val health = (100 * (0.5 * trust + 0.3 * fairness + 0.2 * recency)).roundToInt().coerceIn(0, 100)

        // --- Öncelik ---
        val extra = when (stance) {
            Stance.OWE -> min(20.0, abs(net).toDouble())
            Stance.DORMANT -> min(20.0, (daysSinceLast / relationType.dormantAfterDays - 1.0) * 10.0)
            Stance.ONE_SIDED -> min(15.0, net / 2.0)
            else -> 0.0
        }
        var priority = (stance.basePriority + extra) * importanceFactor(imp)
        // Önemsiz ve uykudaki tanıdıklar listeyi doldurmasın.
        if (stance == Stance.DORMANT && imp <= 2) priority = 0.0

        val advice = adviceFor(
            stance = stance,
            type = relationType,
            net = net,
            trust = trust,
            daysSinceLast = daysSinceLast.toInt(),
            recentTheirLetDowns = recentTheirLetDowns,
            myGiven = myGivenRecent,
            theirGiven = theirGivenRecent,
        )

        return RelationshipAnalysis(
            credit = credit,
            debt = debt,
            net = net,
            trust = trust,
            health = health,
            stance = stance,
            advice = advice,
            priority = priority,
            eventCount = sorted.size,
            lastEventMillis = last,
            recentTheirLetDowns = recentTheirLetDowns,
        )
    }

    /** Kullanıcının bütün ilişkileri üzerinden kişisel karnesi. */
    fun selfReport(
        relations: List<Pair<List<LedgerEvent>, RelationshipAnalysis>>,
        importances: List<Int>,
        nowMillis: Long,
    ): SelfReport {
        val active = relations.filter { !it.second.isEmpty }
        val analyses = active.map { it.second }
        val yearAgo = nowMillis - 365 * DAY_MILLIS
        val recentEvents = active.flatMap { it.first }.filter { it.occurredAtMillis >= yearAgo }

        val iGave = recentEvents.filter { it.kind == EventKind.I_GAVE }.sumOf { it.points }
        val theyGave = recentEvents.filter { it.kind == EventKind.THEY_GAVE }.sumOf { it.points }
        val iLetDown = recentEvents.filter { it.kind == EventKind.I_LET_DOWN }.sumOf { it.points }

        val givingRatio = if (iGave + theyGave >= 5) iGave.toDouble() / max(1, theyGave) else null
        val reliability = if (iGave + iLetDown >= 5) iGave.toDouble() / (iGave + iLetDown) else null

        val openDebts = analyses.count { it.stance == Stance.OWE }
        val repairs = analyses.count { it.stance == Stance.REPAIR }
        val guarded = analyses.count { it.stance == Stance.GUARD }
        val oneSided = analyses.count { it.stance == Stance.ONE_SIDED }
        val healthy = analyses.count { it.stance == Stance.HEALTHY || it.stance == Stance.LENIENT }
        val dormantImportant = relations.indices.count { i ->
            relations[i].second.stance == Stance.DORMANT && importances.getOrElse(i) { 1 } >= 3
        }

        val style = when {
            givingRatio == null -> GiverStyle.UNKNOWN
            givingRatio < 0.7 -> GiverStyle.TAKER_RISK
            givingRatio <= 1.15 -> GiverStyle.MATCHER
            oneSided + guarded >= max(2, (analyses.size * 0.3).toInt()) -> GiverStyle.SELFLESS_GIVER
            else -> GiverStyle.SMART_GIVER
        }

        val tips = buildList {
            if (repairs > 0) add("$repairs ilişkide onarılmamış bir hatan var. Kısa ve net bir özür, telafiden önce gelir.")
            if (openDebts > 0) add("$openDebts kişiye karşılık borcun birikmiş. Karşılık vermek itibarını korur; büyük olması gerekmez.")
            if (dormantImportant > 0) add("Önemli dediğin $dormantImportant ilişki uykuda. Bu hafta birine kısa bir mesaj at.")
            if (guarded > 0) add("$guarded ilişkide sınır koyman öneriliyor. Sınır koymak ilişkiyi bitirmek değildir; kapıyı açık bırak.")
            if (style == GiverStyle.SELFLESS_GIVER) add("Verdiğin emeği karşılık veren insanlara yönlendir. Cömertlik, seçici olduğunda sürdürülebilir.")
            if (style == GiverStyle.MATCHER) add("Arada karşılık beklemeden küçük bir iyilik yap. Sağlam ilişkiler hesabın biraz dışında büyür.")
            if (reliability != null && reliability < 0.8) add("Sözünü tutma oranın %${(reliability * 100).roundToInt()}. Daha az ama tutabileceğin sözler ver.")
            if (isEmpty()) add("Dengeler iyi görünüyor. Bugün en önemli ilişkilerinden birine karşılıksız bir jest yap.")
        }

        return SelfReport(
            totalCredit = analyses.sumOf { max(0, it.net) },
            totalDebt = analyses.sumOf { max(0, -it.net) },
            activeRelations = analyses.size,
            healthyRelations = healthy,
            openDebts = openDebts,
            pendingRepairs = repairs,
            guardedRelations = guarded,
            dormantImportant = dormantImportant,
            givingRatio = givingRatio,
            myReliability = reliability,
            giverStyle = style,
            tips = tips,
        )
    }

    /** Üstel azalma: yaş [halfLifeDays] gün olduğunda ağırlık 0.5 olur. Gelecekteki olaylar 1 sayılır. */
    fun decay(ageMillis: Long, halfLifeDays: Double): Double {
        if (ageMillis <= 0) return 1.0
        val days = ageMillis.toDouble() / DAY_MILLIS
        return 0.5.pow(days / halfLifeDays)
    }

    private fun importanceFactor(importance: Int): Double = 0.6 + 0.2 * importance

    private fun newAdvice(type: RelationType): String =
        if (type.communal) {
            "Bu ilişkide henüz kayıt yok. İlk adımı sen at: ne yaşadığını sor, küçük bir destek teklif et."
        } else {
            "Kazanan stratejiler iyi niyetle başlar. İlk adımı sen at ve karşılığını gözle."
        }

    private fun adviceFor(
        stance: Stance,
        type: RelationType,
        net: Int,
        trust: Double,
        daysSinceLast: Int,
        recentTheirLetDowns: Int,
        myGiven: Double,
        theirGiven: Double,
    ): String {
        val trustPct = (trust * 100).roundToInt()
        return when (stance) {
            Stance.REPAIR ->
                "Son kırgınlığı sen yarattın ve henüz telafi edilmedi. Karşılıklı kırgınlık sarmalını " +
                    "kırmanın en hızlı yolu: kısa, gerekçesiz bir özür ve somut bir telafi."

            Stance.GUARD ->
                "Son dönemde tekrar eden kırgınlıklar var (güven %$trustPct). Açıkça konuş ve bu ilişkiye " +
                    "yatırımını ölçülü olarak azalt. İyi niyet gördüğünde hemen eski haline dön; kapıyı kapatma."

            Stance.OWE ->
                if (type.communal) {
                    "Son dönemde senden çok o verdi (${-net} puan). Hesap gütme ama ihtiyacını sor ve görünür bir destek ol."
                } else {
                    "${-net} puan borçlusun. Karşılık vermek güvenilirliğini ve itibarını korur; " +
                        "orantılı, zamanında bir jest yeterli."
                }

            Stance.ONE_SIDED ->
                if (type.communal) {
                    "Son dönemde daha çok sen veriyorsun. Yakın ilişkide puan saymak yıpratır; bunun yerine " +
                        "neye ihtiyacın olduğunu açıkça söyle."
                } else {
                    val ratio = if (myGiven > 0) (theirGiven / myGiven * 100).roundToInt() else 0
                    "Verdiğinin yalnızca yüzde $ratio kadarı karşılık buluyor. Cömertliğin sınırını aştın: " +
                        "yeni yatırımı karşılığa göre ayarla, gerekirse doğrudan iste."
                }

            Stance.DORMANT ->
                "$daysSinceLast gündür kayıt yok. İlişkinin geleceği zayıfladıkça işbirliği de zayıflar; " +
                    "kısa bir mesaj ya da küçük bir jestle bağı canlandır."

            Stance.LENIENT ->
                if (recentTheirLetDowns == 1) {
                    "Yakın zamanda bir kırgınlık oldu ama tek bir hata kalıp değildir. Hoşgör, iyi niyetle devam et; " +
                        "tekrar ederse konuş."
                } else {
                    "Birkaç kırgınlık var ama genel güven yüksek (%$trustPct). Sorunu yargılamadan konuşmak " +
                        "ilişkiyi korur."
                }

            Stance.NEW -> newAdvice(type)

            Stance.HEALTHY ->
                if (net > 0) {
                    "Sağlıklı bir ortaklık. $net puan alacaklısın ama bu cömertlik payı içinde; hesap sormana gerek yok."
                } else if (net < 0) {
                    "Sağlıklı bir ortaklık. Küçük bir borcun var (${-net} puan); fırsat olduğunda karşılık ver."
                } else {
                    "Sağlıklı ve dengeli bir ortaklık. Arada karşılık beklemeden iyilik yapmaya devam et."
                }
        }
    }
}
