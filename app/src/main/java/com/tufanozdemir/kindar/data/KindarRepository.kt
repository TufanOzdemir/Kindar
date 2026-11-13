package com.tufanozdemir.kindar.data

import androidx.room.withTransaction
import com.tufanozdemir.kindar.domain.EventKind
import com.tufanozdemir.kindar.domain.RelationType
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class KindarRepository(private val db: KindarDatabase) {
    private val dao = db.dao()

    val profiles: Flow<List<ProfileWithEvents>> = dao.observeProfilesWithEvents()

    fun profile(id: Long): Flow<ProfileWithEvents?> = dao.observeProfileWithEvents(id)

    suspend fun addProfile(
        name: String,
        phone: String?,
        relationType: RelationType,
        importance: Int,
        notes: String,
    ): Long = dao.insertProfile(
        ProfileEntity(
            displayName = name.trim(),
            phone = phone?.trim()?.ifBlank { null },
            relationType = relationType.name,
            importance = importance.coerceIn(1, 5),
            notes = notes.trim(),
        ),
    )

    suspend fun updateProfile(profile: ProfileEntity) = dao.updateProfile(profile)

    suspend fun deleteProfile(profile: ProfileEntity) = dao.deleteProfile(profile)

    suspend fun addEvent(
        profileId: Long,
        kind: EventKind,
        points: Int,
        title: String,
        note: String,
        occurredAt: Long,
    ): Long = dao.insertEvent(
        EventEntity(
            profileId = profileId,
            kind = kind.name,
            points = points.coerceIn(1, 10),
            title = title.trim().ifBlank { kind.shortLabel },
            note = note.trim(),
            occurredAt = occurredAt,
        ),
    )

    suspend fun deleteEvent(event: EventEntity) = dao.deleteEvent(event)

    // ---------------------------------------------------------------------
    // Yedekleme: sunucu olmadığı için kullanıcı verisini JSON dosyasına alıp geri yükleyebilir.
    // ---------------------------------------------------------------------

    suspend fun exportJson(): String {
        val profiles = dao.allProfiles()
        val events = dao.allEvents()
        val root = JSONObject()
        root.put("app", "Kindar")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put(
            "profiles",
            JSONArray().apply {
                profiles.forEach { p ->
                    put(
                        JSONObject()
                            .put("id", p.id)
                            .put("displayName", p.displayName)
                            .put("phone", p.phone ?: JSONObject.NULL)
                            .put("contactLookupKey", p.contactLookupKey ?: JSONObject.NULL)
                            .put("contactMissing", p.contactMissing)
                            .put("relationType", p.relationType)
                            .put("importance", p.importance)
                            .put("notes", p.notes)
                            .put("createdAt", p.createdAt),
                    )
                }
            },
        )
        root.put(
            "events",
            JSONArray().apply {
                events.forEach { e ->
                    put(
                        JSONObject()
                            .put("profileId", e.profileId)
                            .put("kind", e.kind)
                            .put("points", e.points)
                            .put("title", e.title)
                            .put("note", e.note)
                            .put("occurredAt", e.occurredAt)
                            .put("createdAt", e.createdAt),
                    )
                }
            },
        )
        return root.toString(2)
    }

    /** Mevcut veriyi yedekteki veriyle değiştirir. Dönen değer: (profil, kayıt) sayısı. */
    suspend fun importJson(json: String): Pair<Int, Int> {
        val root = JSONObject(json)
        require(root.optString("app") == "Kindar") { "Bu dosya bir Kindar yedeği değil." }
        val profilesJson = root.getJSONArray("profiles")
        val eventsJson = root.getJSONArray("events")

        val profiles = (0 until profilesJson.length()).map { i ->
            val o = profilesJson.getJSONObject(i)
            ProfileEntity(
                id = o.getLong("id"),
                displayName = o.getString("displayName"),
                phone = o.optStringOrNull("phone"),
                contactLookupKey = o.optStringOrNull("contactLookupKey"),
                contactMissing = o.optBoolean("contactMissing", false),
                relationType = RelationType.fromName(o.optString("relationType")).name,
                importance = o.optInt("importance", 2).coerceIn(1, 5),
                notes = o.optString("notes", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }
        val ids = profiles.map { it.id }.toHashSet()
        val events = (0 until eventsJson.length()).map { i ->
            val o = eventsJson.getJSONObject(i)
            EventEntity(
                profileId = o.getLong("profileId"),
                kind = EventKind.fromName(o.getString("kind")).name,
                points = o.getInt("points").coerceIn(1, 10),
                title = o.optString("title", ""),
                note = o.optString("note", ""),
                occurredAt = o.getLong("occurredAt"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
            )
        }.filter { it.profileId in ids }

        db.withTransaction {
            dao.clearEvents()
            dao.clearProfiles()
            dao.insertProfiles(profiles)
            dao.insertEvents(events)
        }
        return profiles.size to events.size
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).ifBlank { null }

    companion object {
        const val BACKUP_VERSION = 1
    }
}
