package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Principle(val title: String, val body: String)

private val principles = listOf(
    Principle(
        "1. İyi niyetle başla",
        "Axelrod'un turnuvalarında en başarılı stratejilerin hepsi \"iyi\" idi: ilk ihlali asla kendileri yapmadılar. " +
            "Kindar her yeni ilişkiye güven ön kabulüyle (%67) başlar ve ilk adımı atmanı önerir.",
    ),
    Principle(
        "2. Hoşgörülü ol",
        "Gerçek hayatta yanlış anlamalar olur. Gürültülü ortamlarla yapılan deneylerde kazananlar tek bir ihlale " +
            "hemen karşılık vermeyenlerdi. Tek kırgınlık \"Hoşgör\" önerisi üretir; sınır koymak için tekrar gerekir.",
    ),
    Principle(
        "3. Ölçülü karşılık ver",
        "Hiç karşılık vermeyen strateji sömürülür. Tekrar eden ihlalde Kindar \"Ölçülü sınır koy\" der: konuş ve " +
            "yatırımını azalt, ama ilişkiyi kesme.",
    ),
    Principle(
        "4. Affet",
        "Kin tutan stratejiler (Grim) hatalı bir ortamda kendilerini de cezalandırır. Kindar'da eski olayların güvene " +
            "etkisi her 180 günde yarıya iner.",
    ),
    Principle(
        "5. Cömert ol, ama sınırlı",
        "Cömert Kısasa Kısas (GTFT), karşılıksız kalan hamlelerin yaklaşık üçte birini affeder ve hatalı ortamlarda " +
            "katı stratejileri geçer. Arkadaşlıkta verdiğinin üçte birine kadarı karşılıksız kalabilir; ötesi " +
            "\"Tek taraflı gidiyor\" uyarısıdır.",
    ),
    Principle(
        "6. Kendi hatanı önce onar",
        "Karşılıklı kırgınlık sarmalı en pahalı durumdur. Kendi ihlalin varsa en yüksek öncelik onu onarmaktır.",
    ),
    Principle(
        "7. Borcunu öde, itibarını koru",
        "İnsanlar sadece sana nasıl davrandığına değil, başkalarına nasıl davrandığına da bakar (dolaylı karşılıklılık). " +
            "Aldığın iyiliğe karşılık vermek itibarını korur ve kapıları açar.",
    ),
    Principle(
        "8. Geleceğin gölgesi",
        "İşbirliği ancak ilişkinin geleceği önemliyse sürer. Önemli ilişkileri uykuya bırakma; Kindar seni uyarır.",
    ),
    Principle(
        "9. Yakın ilişkilerde hesap tutma",
        "Psikoloji araştırmaları yakın ilişkilerde sıkı hesap tutmanın ilişkiyi yıprattığını gösterir. " +
            "\"Yakın\" türündeki kişilerde puanlar daha esnek yorumlanır ve öneriler ihtiyaç odaklıdır.",
    ),
)

@Composable
fun GuideScreen(contentPadding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Cömert Karşılıklılık", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Kindar, tekrarlı ilişkilerde uzun vadede kazanan stratejiyi oyun teorisi araştırmalarından alır: " +
                "iyi niyetli, hoşgörülü, gerektiğinde karşılık veren, affeden ve ölçülü cömert. " +
                "Puanlar bir yargı değil, kendi davranışını bilinçli seçmen için bir aynadır.",
            style = MaterialTheme.typography.bodyMedium,
        )
        principles.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(p.title, style = MaterialTheme.typography.titleSmall)
                    Text(p.body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Text("Puanlar nasıl işler?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            "• Sen iyilik yaptığında ya da o seni kırdığında alacağın artar (+).\n" +
                "• O sana iyilik yaptığında ya da sen onu kırdığında borcun artar (−).\n" +
                "• Net puan = alacak − borç.\n" +
                "• Güven, karşı tarafın yakın geçmişteki iyilik ve kırgınlıklarından hesaplanır.\n" +
                "• \"Bugün\" ekranı, ilişkinin önemine göre en çok fark yaratacak hamleyi öne çıkarır.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
