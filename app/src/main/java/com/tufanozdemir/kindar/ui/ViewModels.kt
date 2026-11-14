package com.tufanozdemir.kindar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tufanozdemir.kindar.KindarApp
import com.tufanozdemir.kindar.data.EventEntity
import com.tufanozdemir.kindar.data.ProfileEntity
import com.tufanozdemir.kindar.data.ProfileWithEvents
import com.tufanozdemir.kindar.data.relation
import com.tufanozdemir.kindar.data.toLedger
import com.tufanozdemir.kindar.domain.EventKind
import com.tufanozdemir.kindar.domain.ReciprocityEngine
import com.tufanozdemir.kindar.domain.RelationType
import com.tufanozdemir.kindar.domain.RelationshipAnalysis
import com.tufanozdemir.kindar.domain.SelfReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileSummary(
    val profile: ProfileEntity,
    val analysis: RelationshipAnalysis,
)

fun ProfileWithEvents.analyze(now: Long = System.currentTimeMillis()): RelationshipAnalysis =
    ReciprocityEngine.analyze(
        events = events.map { it.toLedger() },
        relationType = profile.relation,
        importance = profile.importance,
        nowMillis = now,
    )

data class OverviewState(
    val loading: Boolean = true,
    val summaries: List<ProfileSummary> = emptyList(),
    val report: SelfReport? = null,
) {
    /** "Bugün" ekranındaki öneriler: önceliği olanlar, en acil önce. */
    val actions: List<ProfileSummary>
        get() = summaries.filter { it.analysis.priority > 0.0 }.sortedByDescending { it.analysis.priority }
}

class MainViewModel(private val app: KindarApp) : ViewModel() {
    private val repository = app.repository

    val state: StateFlow<OverviewState> = repository.profiles
        .map { list ->
            val now = System.currentTimeMillis()
            val pairs = list.map { pw -> pw to pw.analyze(now) }
            val report = ReciprocityEngine.selfReport(
                relations = pairs.map { (pw, a) -> pw.events.map { it.toLedger() } to a },
                importances = pairs.map { it.first.profile.importance },
                nowMillis = now,
            )
            OverviewState(
                loading = false,
                summaries = pairs.map { (pw, a) -> ProfileSummary(pw.profile, a) },
                report = report,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun hasContactsPermission(): Boolean = app.contactsSync.hasPermission()

    fun onContactsPermissionGranted() {
        app.startContactSyncIfAllowed()
        syncContacts()
    }

    fun syncContacts() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            val result = runCatching { app.contactsSync.sync() }
            _syncing.value = false
            _message.value = result.fold({ it.message }, { "Senkronizasyon başarısız: ${it.message}" })
        }
    }

    fun addProfile(
        name: String,
        phone: String?,
        type: RelationType,
        importance: Int,
        notes: String,
        onCreated: (Long) -> Unit,
    ) {
        viewModelScope.launch {
            val id = repository.addProfile(name, phone, type, importance, notes)
            onCreated(id)
        }
    }

    fun exportTo(write: suspend (String) -> Unit) {
        viewModelScope.launch {
            _message.value = runCatching {
                write(repository.exportJson())
                "Yedek kaydedildi."
            }.getOrElse { "Yedek kaydedilemedi: ${it.message}" }
        }
    }

    fun importFrom(read: suspend () -> String) {
        viewModelScope.launch {
            _message.value = runCatching {
                val (p, e) = repository.importJson(read())
                "Yedek yüklendi: $p kişi, $e kayıt."
            }.getOrElse { "Yedek yüklenemedi: ${it.message}" }
        }
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    fun consumeMessage() {
        _message.value = null
    }
}

data class ProfileDetailState(
    val loading: Boolean = true,
    val profile: ProfileEntity? = null,
    val events: List<EventEntity> = emptyList(),
    val analysis: RelationshipAnalysis? = null,
)

class ProfileViewModel(private val app: KindarApp, val profileId: Long) : ViewModel() {
    private val repository = app.repository

    val state: StateFlow<ProfileDetailState> = repository.profile(profileId)
        .map { pw ->
            if (pw == null) {
                ProfileDetailState(loading = false)
            } else {
                ProfileDetailState(
                    loading = false,
                    profile = pw.profile,
                    events = pw.events.sortedByDescending { it.occurredAt },
                    analysis = pw.analyze(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileDetailState())

    /** Ekran hemen kapandığı için kayıt uygulama kapsamında yazılır; ViewModel temizlense de iptal olmaz. */
    fun addEvent(kind: EventKind, points: Int, title: String, note: String, occurredAt: Long) {
        app.appScope.launch { repository.addEvent(profileId, kind, points, title, note, occurredAt) }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch { repository.deleteEvent(event) }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch { repository.updateProfile(profile) }
    }

    fun deleteProfile(onDone: () -> Unit) {
        val p = state.value.profile ?: return
        viewModelScope.launch {
            repository.deleteProfile(p)
            onDone()
        }
    }
}
