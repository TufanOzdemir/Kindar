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

Her pull request'te GitHub Actions testleri çalıştırır, veritabanı şemasını denetler ve release APK'yı derler.

## Release ve güncellemeler

`main` dalına giren her değişiklikte iş akışı imzalı bir release APK derler ve onu **GitHub Releases** sayfasına `kindar-0.1.<N>.apk` olarak ekler. Telefonda en yeni APK'yı açıp kurmak yeterli: aynı anahtarla imzalandığı ve sürüm kodu her derlemede arttığı için önceki sürümün üzerine kurulur, veriler korunur. Uygulamayı silmeye gerek yoktur.

Verinin güncellemelerde korunması için:

- **Sabit imza:** Her release aynı anahtarla imzalanır. Anahtar repoda durmaz, GitHub secret'larından gelir.
- **Artan sürüm kodu:** `versionCode` = iş akışının çalışma numarası.
- **Veritabanı göçleri:** Şema `app/schemas/` altında saklanır. Şema değişip de dosya güncellenmezse CI durur. Şema değişikliğinde `KindarDatabase.version` arttırılır ve bir göç eklenir. Veriyi silen `fallbackToDestructiveMigration` kullanılmaz.
- **Debug ayrı uygulamadır:** Debug derlemeleri `com.tufanozdemir.kindar.debug` kimliğiyle "Kindar Debug" adında kurulur, gerçek veriye dokunmaz.

### İmza anahtarını kurmak (bir kez)

1. Anahtar oluştur (Android Studio'nun içindeki `jbr/bin/keytool` da olur):
   ```bash
   keytool -genkeypair -keystore kindar-release.jks -storetype PKCS12 -alias kindar \
     -keyalg RSA -keysize 4096 -validity 36500 -dname "CN=Kindar"
   ```
2. Dosyayı base64'e çevir:
   - macOS: `base64 -i kindar-release.jks | pbcopy`
   - Linux: `base64 -w0 kindar-release.jks`
   - Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("kindar-release.jks")) | Set-Clipboard`
3. GitHub'da Settings > Secrets and variables > Actions altında dört secret ekle: `KINDAR_KEYSTORE_BASE64` (2. adımın çıktısı), `KINDAR_KEYSTORE_PASSWORD`, `KINDAR_KEY_ALIAS` (`kindar`), `KINDAR_KEY_PASSWORD` (PKCS12'de şifre ile aynı).
4. `kindar-release.jks` dosyasını ve şifreyi güvenli bir yerde yedekle. Anahtar kaybolursa yeni sürümler eskisinin üzerine kurulamaz.

Secret'lar yoksa iş akışı APK'yı imzasız derler ve yayınlamaz.
