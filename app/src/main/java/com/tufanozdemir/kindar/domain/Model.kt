package com.tufanozdemir.kindar.domain

/**
 * Kindar'ın alan modeli. Bu paket Android'e bağımlı değildir; saf Kotlin'dir ve
 * birim testleriyle doğrulanır.
 *
 * Model "cömert karşılıklılık" (Generous Tit-for-Tat / "partner" stratejileri)
 * üzerine kuruludur. Ayrıntılar için docs/OYUN_TEORISI.md dosyasına bakın.
 */

/** Bir kayıt (olay) türü. Tekrarlı oyunlardaki "işbirliği" ve "ihlal" hamlelerine karşılık gelir. */
enum class EventKind(
    val label: String,
    val shortLabel: String,
    /** Bu olay benim alacağımı mı (+) yoksa borcumu mu (−) arttırır? */
    val ledgerSign: Int,
    /** Hamleyi kim yaptı? */
    val actor: Actor,
    /** İşbirliği mi, ihlal mi? */
    val cooperative: Boolean,
) {
    I_GAVE(
        label = "Ben ona iyilik / destek yaptım",
        shortLabel = "Ben verdim",
        ledgerSign = +1,
        actor = Actor.ME,
        cooperative = true,
    ),
    THEY_GAVE(
        label = "O bana iyilik / destek yaptı",
        shortLabel = "O verdi",
        ledgerSign = -1,
        actor = Actor.THEM,
        cooperative = true,
    ),
    THEY_LET_DOWN(
        label = "O beni kırdı / sözünü tutmadı",
        shortLabel = "O kırdı",
        ledgerSign = +1,
        actor = Actor.THEM,
        cooperative = false,
    ),
    I_LET_DOWN(
        label = "Ben onu kırdım / sözümü tutmadım",
        shortLabel = "Ben kırdım",
        ledgerSign = -1,
        actor = Actor.ME,
        cooperative = false,
    );

    companion object {
        fun fromName(name: String): EventKind = entries.firstOrNull { it.name == name } ?: I_GAVE
    }
}

enum class Actor { ME, THEM }

/**
 * İlişki türü. Psikoloji araştırmaları (Clark & Mills) yakın/"komünal" ilişkilerde sıkı hesap
 * tutmanın ilişkiyi yıprattığını, alışveriş/"exchange" ilişkilerinde ise dengenin beklendiğini
 * gösteriyor. Bu yüzden her tür farklı bir hoşgörü (cömertlik) payı ve farklı bir dil kullanır.
 */
enum class RelationType(
    val label: String,
    /**
     * Karşılıksız kalabilecek verme oranı. Generous TFT'nin optimal bağışlama olasılığı standart
     * mahkum ikilemi ödemelerinde 1/3'tür; arkadaşlık için bu değeri temel alıyoruz.
     */
    val generosityTolerance: Double,
    /** Bu kadar gün temas yoksa ilişki "uykuda" sayılır. */
    val dormantAfterDays: Int,
    /** Yakın ilişki mi (puan dili yumuşatılır, ihtiyaç odaklı öneriler verilir)? */
    val communal: Boolean,
) {
    CLOSE("Yakın (aile, can dostu)", generosityTolerance = 0.50, dormantAfterDays = 21, communal = true),
    FRIEND("Arkadaş", generosityTolerance = 1.0 / 3.0, dormantAfterDays = 60, communal = false),
    WORK("İş / profesyonel", generosityTolerance = 0.25, dormantAfterDays = 90, communal = false),
    ACQUAINTANCE("Tanıdık", generosityTolerance = 0.25, dormantAfterDays = 180, communal = false);

    companion object {
        fun fromName(name: String): RelationType = entries.firstOrNull { it.name == name } ?: ACQUAINTANCE
    }
}

/** Analiz için gereken en küçük olay bilgisi. */
data class LedgerEvent(
    val kind: EventKind,
    /** 1..10 arası büyüklük. */
    val points: Int,
    val occurredAtMillis: Long,
)

/**
 * Önerilen strateji / duruş. Sıralama önceliğe göre değil; öncelik [Stance.basePriority] ile verilir.
 */
enum class Stance(
    val title: String,
    val basePriority: Int,
    val tone: Tone,
) {
    REPAIR("Onar", 100, Tone.ACTION),
    OWE("Sıra sende", 80, Tone.ACTION),
    GUARD("Ölçülü sınır koy", 70, Tone.WARNING),
    ONE_SIDED("Tek taraflı gidiyor", 60, Tone.WARNING),
    DORMANT("Yeniden bağlan", 40, Tone.ACTION),
    LENIENT("Hoşgör", 30, Tone.NEUTRAL),
    NEW("İlk adımı at", 10, Tone.NEUTRAL),
    HEALTHY("Sağlıklı ortaklık", 0, Tone.POSITIVE);
}

enum class Tone { POSITIVE, NEUTRAL, ACTION, WARNING }

/** Tek bir kişiyle ilişkinin analiz sonucu. */
data class RelationshipAnalysis(
    /** Benim verdiğim + bana yapılan ihlaller (puan). */
    val credit: Int,
    /** Bana verilen + benim yaptığım ihlaller (puan). */
    val debt: Int,
    /** Net puan: pozitifse alacaklıyım, negatifse borçluyum. */
    val net: Int,
    /** Karşı tarafın işbirliği eğilimi tahmini (0..1), zamanla affedilen (azalan) geçmişle. */
    val trust: Double,
    /** İlişki sağlığı 0..100. */
    val health: Int,
    val stance: Stance,
    /** Kişiye özel, uygulanabilir öneri metni. */
    val advice: String,
    /** "Bugün" listesindeki sıralama puanı. */
    val priority: Double,
    val eventCount: Int,
    val lastEventMillis: Long?,
    /** Son 90 günde karşı tarafın ihlal sayısı. */
    val recentTheirLetDowns: Int,
) {
    val isEmpty: Boolean get() = eventCount == 0
}

/** Kullanıcının kendi "karnesi": bütün ilişkiler üzerinden. */
data class SelfReport(
    val totalCredit: Int,
    val totalDebt: Int,
    val activeRelations: Int,
    val healthyRelations: Int,
    val openDebts: Int,
    val pendingRepairs: Int,
    val guardedRelations: Int,
    val dormantImportant: Int,
    /** Verdiğim / aldığım (işbirliği puanları), null ise henüz veri yok. */
    val givingRatio: Double?,
    /** Benim sözümü tutma oranım (0..1), null ise veri yok. */
    val myReliability: Double?,
    val giverStyle: GiverStyle,
    val tips: List<String>,
)

/** Adam Grant'in Give and Take sınıflandırmasından esinlenen kullanıcı tarzı. */
enum class GiverStyle(val title: String, val description: String) {
    UNKNOWN(
        "Henüz belirsiz",
        "Birkaç kayıt girdikçe ilişki tarzın burada görünecek.",
    ),
    SMART_GIVER(
        "Akıllı verici",
        "Cömertsin ve aynı zamanda kendini koruyorsun. Uzun vadede en başarılı tarz bu.",
    ),
    SELFLESS_GIVER(
        "Fedakâr verici",
        "Çok veriyorsun ama karşılıksız kalan ilişkiler birikiyor. Tükenmemek için verdiğin yeri seç.",
    ),
    MATCHER(
        "Denkleştirici",
        "Aldığın kadar veriyorsun. Adil ama arada karşılık beklemeden vermek ilişkileri büyütür.",
    ),
    TAKER_RISK(
        "Alan tarafta",
        "Son dönemde aldıkların verdiklerinden belirgin fazla. Küçük jestlerle dengeyi kurmak itibarını korur.",
    ),
}
