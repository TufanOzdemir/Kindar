# Kindar'ın modeli: Cömert Karşılıklılık

Bu belge, Kindar'ın puanlama, borç/alacak ve öneri mantığının hangi araştırmalara dayandığını ve bu araştırmaların koda nasıl çevrildiğini anlatır. Kodun karşılığı `app/src/main/java/com/tufanozdemir/kindar/domain/ReciprocityEngine.kt` dosyasıdır.

## Kısa cevap

Tekrarlı ilişkilerde uzun vadede kazanan model **cömert karşılıklılıktır**: iyi niyetle başla, iyiliğe iyilikle karşılık ver, tek bir hatayı hoşgör, tekrar eden ihlale ölçülü karşılık ver, affet ve karşılıksız kalan küçük bir payı bilerek cömertçe bırak. Kendi hatanı hızla onar, aldığın iyiliğin karşılığını ver ve önemli ilişkileri uykuya bırakma.

Bu, oyun teorisinin "Kısasa Kısas" (Tit-for-Tat) stratejisinin hatalara dayanıklı hale getirilmiş halidir. Saf Kısasa Kısas, gerçek hayattaki gibi yanlış anlamaların olduğu ortamlarda kısır döngülere girer; onu geçen stratejiler daha cömert ve daha affedicidir.

## 1. Çerçeve: Tekrarlı Mahkum İkilemi

İki kişi arasındaki her etkileşimde her iki taraf da işbirliği yapabilir (iyilik, destek, söz tutma) ya da ihlal edebilir (bencillik, söz bozma). Tek seferlik bir karşılaşmada ihlal etmek bireysel olarak cazip görünür. İlişki tekrarlandığında ise durum değişir: bugünkü davranış yarınki karşılığı belirler.

Bu yüzden işbirliği ancak **geleceğin gölgesi** yeterince uzunsa sürer. Standart ödemelerle (T=5, R=3, P=1, S=0) bir sonraki karşılaşmanın olasılığı w, w ≥ (T−R)/(T−P) = 0,5 olduğunda karşılıklı işbirliği kararlı hale gelir (Axelrod 1984). Pratikte bu, **ilişkinin gelecekteki önemi arttıkça işbirliğine yatırım yapmanın daha akıllıca olduğu** anlamına gelir.

**Kindar'da:** Her profilin 1 ile 5 arası bir "gelecekteki önemi" vardır. Önerilerin önceliği bu değerle çarpılır, önemli ve uykuda kalan ilişkiler öne çıkarılır.

## 2. Axelrod turnuvaları: Kısasa Kısas neden kazandı?

Robert Axelrod 1980'lerde oyun teorisyenlerinden strateji topladı ve bunları birbirine karşı oynattı. İki turnuvayı da en basit strateji, Anatol Rapoport'un **Kısasa Kısas**'ı kazandı: ilk hamlede işbirliği yap, sonra karşı tarafın son hamlesini tekrarla.

Axelrod başarılı stratejilerin dört ortak özelliğini çıkardı:

1. **İyi niyetli:** İlk ihlali asla kendin yapma.
2. **Karşılık veren:** İhlale tepkisiz kalma, yoksa sömürülürsün.
3. **Affedici:** Karşı taraf düzelince sen de düzel, kin tutma.
4. **Açık:** Davranışın öngörülebilir olsun ki karşı taraf işbirliğinin işe yaradığını anlasın.

Önemli bir ayrıntı: Kısasa Kısas hiçbir ikili karşılaşmada rakibinden fazla puan almaz, ama toplamda kazanır. Hayatta başarı, karşındakini yenmekten değil, birçok ilişkide karşılıklı kazanç yaratmaktan gelir.

## 3. Gerçek hayat gürültülüdür: Cömert Kısasa Kısas

Kısasa Kısas'ın zayıf noktası **hatalardır**. Bir mesaj yanlış anlaşıldığında ya da bir söz istemeden tutulamadığında, iki Kısasa Kısas oyuncusu birbirini sonsuza kadar cezalandıran bir yankı döngüsüne girer.

Nowak ve Sigmund (1992), hatalı ortamlarda evrimin **Cömert Kısasa Kısas**'ı (Generous TFT, GTFT) seçtiğini gösterdi: işbirliğine her zaman işbirliğiyle karşılık ver, ihlale ise belirli bir olasılıkla yine de işbirliğiyle karşılık ver. Standart ödemelerde en uygun bağışlama olasılığı:

> q = min{ 1 − (T−R)/(R−S), (R−P)/(T−P) } = min{ 1/3, 1/2 } = **1/3**

Yani karşılıksız kalan hamlelerin yaklaşık **üçte biri** affedilebilir; bu kadar cömertlik hataların yarattığı sarmalları kırar ama sömürülmeye davetiye çıkarmaz.

**Kindar'da:** Arkadaşlık ilişkilerinde verdiğinin üçte birine kadarı karşılıksız kalabilir; buna "cömertlik payı" diyoruz. Bu payın ötesine geçildiğinde öneri "Tek taraflı gidiyor" olur. Pay ilişki türüne göre değişir: yakın ilişkilerde 1/2, arkadaşta 1/3, iş ve tanıdıklarda 1/4.

## 4. Kendi hatanı onar: Win-Stay, Lose-Shift

Nowak ve Sigmund (1993) bir başka güçlü stratejiyi, **Win-Stay, Lose-Shift**'i (WSLS, "Pavlov") öne çıkardı: işler iyi gidiyorsa aynen devam et, kötü gidiyorsa davranışını değiştir. WSLS'nin en değerli özelliği, karşılıklı kırgınlıktan **tek adımda** çıkabilmesidir: iki taraf da ihlal ettiyse, ikisi de işbirliğine döner.

**Kindar'da:** Kendi yaptığın bir kırgınlık son 90 gün içindeyse ve henüz telafi etmediysen, en yüksek öncelikli öneri "Onar" olur. Telafi edilene, yani sonrasında en az aynı büyüklükte bir iyilik kaydedilene kadar bu öneri kalır.

## 5. İnsanlarla yapılan deneyler: hoşgörü ve affetme kazanır

Fudenberg, Rand ve Dreber (2012, *American Economic Review*), hatalı (gürültülü) tekrarlı Mahkum İkilemi'ni insanlarla oynattı. Bulgular:

- İşbirliğinin denge olduğu ortamlarda işbirliği yapan stratejiler, işbirliği yapmayanlardan daha yüksek kazanç sağladı.
- Başarılı stratejiler **hoşgörülüydü (lenient):** ilk ihlale hemen karşılık vermediler. En yaygın başarılı stratejiler "iki ihlalde bir karşılık" (Tit-for-2-Tats) gibi stratejilerdi.
- Birçoğu **affediciydi (forgiving):** bir ceza verdikten sonra işbirliğine dönmeye çalıştılar.
- Yazarlar bu hoşgörünün sosyal tercihten değil, stratejik akıldan geldiği sonucuna vardı.

**Kindar'da:** Son 90 günde tek bir kırgınlık varsa öneri "Hoşgör" olur: tek bir hata kalıp değildir. "Ölçülü sınır koy" önerisi için en az iki kırgınlık ve düşmüş bir güven gerekir.

## 6. Sömürü kaybeder, cömertlik kazanır

Press ve Dyson (2012), "sıfır-determinant" denen ve karşı tarafın kazancını tek taraflı belirleyebilen stratejileri keşfetti. Bunların içinde rakibi sömüren "zorba" (extortion) stratejiler de vardı. Stewart ve Plotkin (2013, *PNAS*) bu stratejilerin evrimsel ortamda nasıl davrandığını inceledi: **zorba stratejiler başarısız oldu, cömert olanlar kazandı.** Cömert stratejiler, en küçük topluluklar dışında, yerlerinin ele geçirilmesine dirençliydi ve işbirliği yapmayan stratejilerin yerini alabildi.

Hilbe, Chatterjee ve Nowak (2018, *Nature Human Behaviour*) evrimin iki tür strateji ürettiğini gösterdi: **ortaklar (partners)** ve **rakipler (rivals)**. Ortaklar karşılıklı işbirliğini hedefler; ara sıra sömürülmeyi göze alırlar ama karşı tarafın hiçbir zaman kendilerinden fazlasını uzun süre almasına izin vermezler. Rakipler ise asla geride kalmamaya odaklanır ve işbirliğini çökertir.

**Kindar'da:** Model bilerek bir "ortak" stratejisidir. Hedef karşı tarafı yenmek değil, ilişkiyi karşılıklı kazanca taşımaktır. Ama karşılıksız kalan verme cömertlik payını aştığında ya da güven düştüğünde kendini korumayı önerir.

## 7. İtibar: başkaları da izliyor

Nowak ve Sigmund (1998, 2005) **dolaylı karşılıklılığı** tanımladı: "Ben sana yardım edersem, bir başkası da bana yardım eder." İnsanlar yalnızca kendilerine nasıl davranıldığına değil, senin başkalarına nasıl davrandığına da bakar. İyi itibar, hiç tanımadığın insanlarla bile işbirliği kapısını açar.

**Kindar'da:** Borçlu olduğun ilişkiler "Sıra sende" önerisiyle öne çıkar. Karnem ekranındaki "sözünü tutma oranı", itibarının sana dönük bir göstergesidir.

## 8. Psikoloji: yakın ilişkilerde hesap tutma

Oyun teorisi alışveriş ilişkilerini iyi açıklar, ama yakın ilişkiler farklı işler. Clark ve Mills, **komünal** (aile, yakın dost) ilişkilerde insanların karşı tarafın ihtiyacına baktığını, **alışveriş** (iş, tanıdık) ilişkilerinde ise verilen ve alınanı takip ettiğini gösterdi. Komünal bir ilişkide sıkı hesap tutmak, ilişkinin yakın olmadığı sinyalini verir ve onu yıpratır.

**Kindar'da:** "Yakın" türündeki kişilerde borç uyarısı daha geç gelir (6 puan, diğerlerinde 3 puan), cömertlik payı en geniştir (1/2) ve öneriler puan değil ihtiyaç dilindedir ("ihtiyacını sor", "neye ihtiyacın olduğunu söyle").

## 9. Kişisel başarı: akıllı vericiler

Adam Grant (*Give and Take*, 2013) iş hayatındaki insanları üç gruba ayırdı: **vericiler**, **denkleştiriciler** ve **alıcılar**. Başarı sıralamasının hem en altında hem de en üstünde vericiler vardı. Fark şuydu: en altta kalan "fedakâr" vericiler herkese sınırsız verip tükeniyordu; en üstteki "akıllı" (otherish) vericiler ise cömertti ama kime verdiklerini seçiyor ve alıcılara karşı kendilerini koruyorlardı. Bu, oyun teorisindeki cömert ama karşılık veren ortak stratejisinin iş hayatındaki karşılığıdır.

Hayat kalitesi tarafında, Holt-Lunstad ve arkadaşlarının (2010, *PLoS Medicine*) 148 çalışmalık meta-analizi, güçlü sosyal ilişkilerin hayatta kalma olasılığını yaklaşık %50 arttırdığını gösterdi. Harvard Yetişkin Gelişimi Çalışması da uzun ve mutlu bir hayatın en güçlü belirleyicisinin ilişkilerin kalitesi olduğunu buldu. İlişkilere bilinçli yatırım yapmak doğrudan hayat kalitesine yatırımdır.

**Kindar'da:** Karnem ekranı son bir yılın verme/alma oranından tarzını çıkarır: akıllı verici, fedakâr verici, denkleştirici ya da alan taraf. Fedakâr vericiye "verdiğin yeri seç", denkleştiriciye "arada karşılık beklemeden ver" gibi öneriler sunar.

## 10. Kendi simülasyonumuz

Modeli doğrulamak için `TournamentSimulationTest.kt` içinde %5 hata (yanlış anlama) olan bir Axelrod turnuvası ve evrimsel (replikatör) dinamik çalıştırdık. 9 strateji, her eşleşme 200 tur ve 30 tekrar, ödemeler T=5, R=3, P=1, S=0.

"KINDAR" stratejisi uygulamanın mantığının sadeleştirilmiş halidir: karşı tarafın güvenini hafızası azalan bir ortalamayla izler, tek hatayı affeder, güven yeterliyse ihlallerin üçte birini bağışlar, güven düşükse karşılık verir.

**Turnuva (tur başına ortalama puan):**

| Sıra | Strateji | Puan |
|---|---|---|
| 1 | KINDAR | 2,348 |
| 2 | TFT (Kısasa Kısas) | 2,330 |
| 3 | TF2T (iki ihlalde bir karşılık) | 2,304 |
| 4 | WSLS | 2,299 |
| 5 | GTFT (Cömert Kısasa Kısas) | 2,299 |
| 6 | RANDOM | 2,285 |
| 7 | GRIM (asla affetmeyen) | 2,203 |
| 8 | ALLD (hep ihlal eden) | 2,160 |
| 9 | ALLC (hep işbirliği yapan) | 2,035 |

**Sızma testi:** Herkesin KINDAR oynadığı bir toplulukta KINDAR tur başına 2,95 puan alır. Sızmaya çalışan stratejilerin aldığı puanlar ALLD 1,33, GRIM 1,49, WSLS 2,49 ve RANDOM 2,72'dir. Hiçbiri yerleşemez.

**Evrimsel dinamik:** Hep ihlal eden ve rastgele stratejiler yok olur, nüfus cömert karşılık verenlere (GTFT ve benzerleri) geçer.

**Dürüst sınırlar:**
- İyi niyetli stratejiler (TFT, GTFT, KINDAR) birbirleriyle karşılaştıklarında hep işbirliği yaparlar ve aralarındaki fark gürültü düzeyindedir. Evrimsel simülasyonda nüfusun çoğu GTFT'de toplandı; bu, KINDAR'ın değil, "cömert karşılıklılık" ailesinin kazandığı anlamına gelir.
- Hep işbirliği yapan (ALLC) stratejilerin çok olduğu bir başlangıçta, GRIM gibi bir "rakip" strateji onları sömürerek yayılabilir. Bu, Hilbe ve arkadaşlarının ortak ve rakip sonucuyla tutarlıdır ve koşulsuz cömertliğin neden riskli olduğunu gösterir.
- İnsan ilişkileri oyun teorisi modellerinden zengindir. Puanlar karar vermene yardım eden bir aynadır, bir yargı değildir.

## 11. Kurallar özeti (koddaki karşılıklarıyla)

| İlke | Kaynak | Kindar'daki karşılığı |
|---|---|---|
| İyi niyetle başla | Axelrod | Başlangıç güveni 2/3; kayıt yoksa "İlk adımı at" |
| Hoşgörülü ol | Fudenberg, Rand ve Dreber | Tek kırgınlık "Hoşgör" önerir |
| Karşılık ver | Axelrod; Hilbe ve ark. | 2+ kırgınlık ve düşük güvende "Ölçülü sınır koy" |
| Affet | Nowak ve Sigmund | Güven hesabında 180 günlük yarı ömür |
| Cömert ol | GTFT; Stewart ve Plotkin | İlişki türüne göre cömertlik payı (arkadaşta 1/3) |
| Hatanı onar | WSLS | Telafi edilmemiş kendi kırgınlığın "Onar" (en yüksek öncelik) |
| Borcunu öde | Dolaylı karşılıklılık | Net borç eşiği aşınca "Sıra sende" |
| Geleceğin gölgesi | Axelrod | Önem çarpanı; önemli ilişkiler uykudaysa "Yeniden bağlan" |
| Yakında hesap tutma | Clark ve Mills | Yakın ilişkilerde yumuşak eşikler ve ihtiyaç odaklı dil |
| Akıllı verici ol | Grant | Karnem ekranındaki tarz ve öneriler |

## Puanlama formülleri

- **Alacak** = Σ(benim yaptığım iyilikler) + Σ(onun yaptığı kırgınlıklar)
- **Borç** = Σ(onun yaptığı iyilikler) + Σ(benim yaptığım kırgınlıklar)
- **Net** = Alacak − Borç (pozitifse alacaklısın, negatifse borçlusun)
- **Güven** = (İ + 4) / (İ + K + 6), burada İ ve K karşı tarafın iyilik ve kırgınlık puanlarının zamanla azalan toplamıdır (ağırlık = 0,5^(gün/180))
- **Sağlık** = 100 × (0,5 × güven + 0,3 × denge + 0,2 × güncellik)
- **Öncelik** = (duruşun temel önceliği + ek) × (0,6 + 0,2 × önem)

## Kaynaklar

- Axelrod, R. (1984). *The Evolution of Cooperation*. Basic Books.
- Nowak, M. A. ve Sigmund, K. (1992). Tit for tat in heterogeneous populations. *Nature*, 355, 250–253.
- Nowak, M. A. ve Sigmund, K. (1993). A strategy of win-stay, lose-shift that outperforms tit-for-tat in the Prisoner's Dilemma game. *Nature*, 364, 56–58.
- Nowak, M. A. ve Sigmund, K. (2005). Evolution of indirect reciprocity. *Nature*, 437, 1291–1298.
- Nowak, M. A. (2006). Five rules for the evolution of cooperation. *Science*, 314, 1560–1563.
- Fudenberg, D., Rand, D. G. ve Dreber, A. (2012). Slow to anger and fast to forgive: Cooperation in an uncertain world. *American Economic Review*, 102(2), 720–749. https://economics.mit.edu/research/publications/slow-anger-and-fast-forgive-cooperation-uncertain-world
- Press, W. H. ve Dyson, F. J. (2012). Iterated Prisoner's Dilemma contains strategies that dominate any evolutionary opponent. *PNAS*, 109, 10409–10413.
- Stewart, A. J. ve Plotkin, J. B. (2013). From extortion to generosity, evolution in the Iterated Prisoner's Dilemma. *PNAS*, 110, 15348–15353. https://arxiv.org/abs/1304.7205
- Hilbe, C., Chatterjee, K. ve Nowak, M. A. (2018). Partners and rivals in direct reciprocity. *Nature Human Behaviour*, 2, 469–477. https://research-explorer.ista.ac.at/record/419
- Clark, M. S. ve Mills, J. (1979, 1993). Communal and exchange relationships. *Journal of Personality and Social Psychology*; *Personality and Social Psychology Bulletin*, 19(6). https://journals.sagepub.com/doi/10.1177/0146167293196002
- Grant, A. (2013). *Give and Take: Why Helping Others Drives Our Success*. Viking.
- Holt-Lunstad, J., Smith, T. B. ve Layton, J. B. (2010). Social relationships and mortality risk: a meta-analytic review. *PLoS Medicine*, 7(7), e1000316.
