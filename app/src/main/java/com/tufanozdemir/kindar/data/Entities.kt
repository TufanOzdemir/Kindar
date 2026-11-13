package com.tufanozdemir.kindar.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.tufanozdemir.kindar.domain.EventKind
import com.tufanozdemir.kindar.domain.LedgerEvent
import com.tufanozdemir.kindar.domain.RelationType

/** Bir kişi profili. Rehberden gelebilir ya da elle eklenebilir. */
@Entity(
    tableName = "profiles",
    indices = [Index(value = ["contactLookupKey"], unique = true)],
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val phone: String? = null,
    val photoUri: String? = null,
    /** Rehberdeki kişinin kalıcı anahtarı (ContactsContract.Contacts.LOOKUP_KEY). */
    val contactLookupKey: String? = null,
    /** Kişi rehberden silindiyse geçmişi korumak için profil kalır ve bu alan true olur. */
    val contactMissing: Boolean = false,
    @ColumnInfo(defaultValue = "ACQUAINTANCE") val relationType: String = RelationType.ACQUAINTANCE.name,
    /** 1..5: bu ilişkinin gelecekteki önemi ("geleceğin gölgesi"). */
    @ColumnInfo(defaultValue = "2") val importance: Int = 2,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

val ProfileEntity.relation: RelationType get() = RelationType.fromName(relationType)
val ProfileEntity.fromContacts: Boolean get() = contactLookupKey != null

/** Profile girilen bir olay kaydı. */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId"), Index("occurredAt")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val kind: String,
    /** 1..10 */
    val points: Int,
    val title: String,
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

val EventEntity.eventKind: EventKind get() = EventKind.fromName(kind)

fun EventEntity.toLedger(): LedgerEvent = LedgerEvent(eventKind, points, occurredAt)

data class ProfileWithEvents(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val events: List<EventEntity>,
)
