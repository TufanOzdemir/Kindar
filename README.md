# Kindar

Kindar, hayatındaki insanlarla aranda geçenleri not aldığın, kime ne kadar borçlu ya da alacaklı olduğunu puanlarla gördüğün ve ilişkilerine bilinçli yatırım yapman için öneriler alan bir Android adres defteridir.

Puanlama ve öneri mantığı oyun teorisinde tekrarlı ilişkilerde uzun vadede kazanan modele, **cömert karşılıklılığa** dayanır: iyi niyetle başla, iyiliğe karşılık ver, tek hatayı hoşgör, tekrar eden ihlale ölçülü karşılık ver, affet ve ölçülü cömert ol. Araştırmanın tamamı ve formüller için [docs/OYUN_TEORISI.md](docs/OYUN_TEORISI.md) dosyasına bak.

## Özellikler

- **Rehber senkronizasyonu:** Telefon rehberindeki kişiler (numarası olanlar) otomatik olarak profil olur. Rehberdeki isim, numara ve fotoğraf değişiklikleri uygulamaya yansır. Rehberden silinen bir kişinin kayıtları varsa geçmişi korunur.
- **Kayıtlar:** Her profile dört türde olay girilir: ben iyilik yaptım, o iyilik yaptı, o beni kırdı, ben onu kırdım. Her kaydın 1 ile 10 arası bir puanı, başlığı, ayrıntısı ve tarihi vardır.
- **Borç / alacak:** Her kişi için alacak, borç ve net puan.
- **Güven ve ilişki sağlığı:** Karşı tarafın karşılık verme eğilimi (zamanla affeden bir bellekle) ve 0 ile 100 arası bir sağlık skoru.
- **Önerilen hamle:** Onar, Sıra sende, Ölçülü sınır koy, Tek taraflı gidiyor, Yeniden bağlan, Hoşgör, İlk adımı at ya da Sağlıklı ortaklık.
- **Bugün ekranı:** Bütün ilişkiler arasında en çok fark yaratacak hamleler, ilişkinin önemine göre sıralı.
- **Karnem:** Verme/alma oranın, sözünü tutma oranın, ilişki tarzın ve kişisel öneriler.
- **Strateji rehberi:** Modelin ilkeleri uygulamanın içinde.
- **Gizlilik:** Sunucu ve internet izni yoktur. Veriler cihazdaki Room veritabanında durur, bulut yedeğine dahil edilmez. Telefon değiştirirken Karnem ekranından JSON yedek alınıp geri yüklenebilir.

## Teknik yapı

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- Room (yerel SQLite), KSP
- minSdk 26, targetSdk 35
- `domain/` paketi Android'e bağımlı değildir; puanlama motoru ve turnuva simülasyonu JVM birim testleriyle doğrulanır.

```
app/src/main/java/com/tufanozdemir/kindar/
├── domain/      Puanlama motoru (ReciprocityEngine) ve alan modeli
├── data/        Room varlıkları, DAO, depo ve JSON yedekleme
├── contacts/    Rehber senkronizasyonu
└── ui/          ViewModel'ler, tema, bileşenler ve ekranlar
```

## Derleme

```bash
./gradlew testDebugUnitTest   # birim testleri ve turnuva simülasyonu
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

Her pull request'te GitHub Actions testleri çalıştırır ve debug APK'yı "kindar-debug-apk" adıyla iş akışının çıktılarına ekler.
