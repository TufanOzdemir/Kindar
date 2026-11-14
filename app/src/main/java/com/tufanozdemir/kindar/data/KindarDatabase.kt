package com.tufanozdemir.kindar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * Yerel veritabanı.
 *
 * GÜNCELLEMELERDE VERİ KAYBI OLMAMASI İÇİN KURALLAR:
 *  - Şemada (varlıklarda) bir değişiklik yapıldığında [version] bir arttırılır.
 *  - Ya `autoMigrations` listesine `AutoMigration(from = N, to = N + 1)` eklenir ya da
 *    [MIGRATIONS] listesine elle yazılmış bir [Migration] eklenir.
 *  - `fallbackToDestructiveMigration` ASLA kullanılmaz: göç eksikse uygulama veriyi silmek yerine
 *    hata verir, böylece sorun yayından önce fark edilir.
 *  - Her sürümün şeması `app/schemas/` altında repoda saklanır; CI şema değişip de dosya
 *    güncellenmediğinde derlemeyi durdurur.
 */
@Database(
    entities = [ProfileEntity::class, EventEntity::class],
    version = 1,
    exportSchema = true,
    autoMigrations = [],
)
abstract class KindarDatabase : RoomDatabase() {
    abstract fun dao(): KindarDao

    companion object {
        /** Elle yazılan göçler. Örnek: `object : Migration(1, 2) { override fun migrate(db) { ... } }` */
        val MIGRATIONS: Array<Migration> = emptyArray()

        fun create(context: Context): KindarDatabase =
            Room.databaseBuilder(context.applicationContext, KindarDatabase::class.java, "kindar.db")
                .addMigrations(*MIGRATIONS)
                .build()
    }
}
