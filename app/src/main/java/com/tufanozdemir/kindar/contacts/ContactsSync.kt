package com.tufanozdemir.kindar.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.tufanozdemir.kindar.data.KindarDao
import com.tufanozdemir.kindar.data.ProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class SyncResult(
    val added: Int = 0,
    val updated: Int = 0,
    val markedMissing: Int = 0,
    val removed: Int = 0,
    val permissionMissing: Boolean = false,
) {
    val message: String
        get() = if (permissionMissing) {
            "Rehber izni verilmedi."
        } else {
            buildString {
                append("Rehber senkronize edildi")
                val parts = buildList {
                    if (added > 0) add("$added yeni kişi")
                    if (updated > 0) add("$updated güncelleme")
                    if (markedMissing > 0) add("$markedMissing kişi rehberden silinmiş (geçmişi korundu)")
                }
                if (parts.isNotEmpty()) append(": ").append(parts.joinToString(", "))
                append(".")
            }
        }
}

private data class DeviceContact(
    val lookupKey: String,
    val name: String,
    val phone: String?,
    val photoUri: String?,
)

/**
 * Telefon rehberini profillerle eşitler. Rehberdeki her kişi (telefon numarası olan) bir profile karşılık gelir.
 *
 * - Yeni kişiler profil olarak eklenir.
 * - İsim, numara ve fotoğraf değişiklikleri profile yansır; kullanıcının girdiği ilişki türü, önem ve notlar korunur.
 * - Rehberden silinen kişi kaydı varsa geçmişiyle korunur ve "rehberde yok" olarak işaretlenir; kaydı yoksa kaldırılır.
 *
 * Veri hiçbir yere gönderilmez; yalnızca cihazdaki veritabanına yazılır.
 */
class ContactsSync(
    private val context: Context,
    private val dao: KindarDao,
) {
    private val mutex = Mutex()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun sync(): SyncResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext SyncResult(permissionMissing = true)

            val contacts = readContacts()
            val existing = dao.contactProfiles()
            val byKey = existing.associateBy { it.contactLookupKey }.toMutableMap()
            val seenKeys = HashSet<String>()

            val toInsert = mutableListOf<ProfileEntity>()
            val toUpdate = mutableListOf<ProfileEntity>()

            // Birleştirilen kişilerde LOOKUP_KEY değişebilir; eşleşmeyen kişileri isimle yeniden bağlamayı dene.
            val currentKeys = contacts.map { it.lookupKey }.toHashSet()
            val orphansByName = existing
                .filter { it.contactLookupKey !in currentKeys }
                .groupBy { it.displayName.trim().lowercase() }
                .mapValues { it.value.toMutableList() }

            for (c in contacts) {
                if (!seenKeys.add(c.lookupKey)) continue
                var profile = byKey[c.lookupKey]
                if (profile == null) {
                    val orphan = orphansByName[c.name.trim().lowercase()]?.removeFirstOrNull()
                    if (orphan != null) {
                        byKey.remove(orphan.contactLookupKey)
                        profile = orphan.copy(contactLookupKey = c.lookupKey)
                        byKey[c.lookupKey] = profile
                        toUpdate += profile.copy(
                            displayName = c.name,
                            phone = c.phone,
                            photoUri = c.photoUri,
                            contactMissing = false,
                        )
                        continue
                    }
                }
                if (profile == null) {
                    toInsert += ProfileEntity(
                        displayName = c.name,
                        phone = c.phone,
                        photoUri = c.photoUri,
                        contactLookupKey = c.lookupKey,
                    )
                } else if (
                    profile.displayName != c.name ||
                    profile.phone != c.phone ||
                    profile.photoUri != c.photoUri ||
                    profile.contactMissing
                ) {
                    toUpdate += profile.copy(
                        displayName = c.name,
                        phone = c.phone,
                        photoUri = c.photoUri,
                        contactMissing = false,
                    )
                }
            }

            // Rehberden silinenler
            val updatedIds = toUpdate.map { it.id }.toHashSet()
            val gone = existing.filter { it.contactLookupKey !in seenKeys && it.id !in updatedIds }
            val eventCounts = dao.allEvents().groupingBy { it.profileId }.eachCount()
            var missing = 0
            var removed = 0
            for (p in gone) {
                if ((eventCounts[p.id] ?: 0) == 0 && p.notes.isBlank()) {
                    dao.deleteProfile(p)
                    removed++
                } else if (!p.contactMissing) {
                    toUpdate += p.copy(contactMissing = true)
                    missing++
                }
            }

            if (toInsert.isNotEmpty()) dao.insertProfiles(toInsert)
            if (toUpdate.isNotEmpty()) dao.updateProfiles(toUpdate)

            SyncResult(
                added = toInsert.size,
                updated = toUpdate.size - missing,
                markedMissing = missing,
                removed = removed,
            )
        }
    }

    private fun readContacts(): List<DeviceContact> {
        val resolver = context.contentResolver

        // 1) Telefon numaraları (kişi başına birincil olan ya da ilk numara)
        val phones = HashMap<Long, String>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val primIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val number = cursor.getString(numIdx) ?: continue
                if (cursor.getInt(primIdx) != 0 || !phones.containsKey(id)) phones[id] = number
            }
        }

        // 2) Kişiler
        val result = mutableListOf<DeviceContact>()
        resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
            ),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val keyIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)
            while (cursor.moveToNext()) {
                val key = cursor.getString(keyIdx) ?: continue
                val name = cursor.getString(nameIdx)?.takeIf { it.isNotBlank() } ?: continue
                result += DeviceContact(
                    lookupKey = key,
                    name = name,
                    phone = phones[cursor.getLong(idIdx)],
                    photoUri = cursor.getString(photoIdx),
                )
            }
        }
        return result
    }
}
