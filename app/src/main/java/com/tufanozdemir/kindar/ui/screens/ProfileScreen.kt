package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.data.EventEntity
import com.tufanozdemir.kindar.data.ProfileEntity
import com.tufanozdemir.kindar.data.eventKind
import com.tufanozdemir.kindar.data.fromContacts
import com.tufanozdemir.kindar.data.relation
import com.tufanozdemir.kindar.domain.RelationshipAnalysis
import com.tufanozdemir.kindar.ui.ProfileDetailState
import com.tufanozdemir.kindar.ui.components.Avatar
import com.tufanozdemir.kindar.ui.components.LabeledBar
import com.tufanozdemir.kindar.ui.components.StanceCard
import com.tufanozdemir.kindar.ui.components.formatDate
import com.tufanozdemir.kindar.ui.components.formatNet
import com.tufanozdemir.kindar.ui.components.netSentence
import com.tufanozdemir.kindar.ui.theme.LedgerColors
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileDetailState,
    onBack: () -> Unit,
    onAddEvent: () -> Unit,
    onDeleteEvent: (EventEntity) -> Unit,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: () -> Unit,
) {
    val profile = state.profile
    var editing by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }
    var confirmDeleteProfile by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile?.displayName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (profile != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Profili düzenle")
                        }
                        if (!profile.fromContacts || profile.contactMissing) {
                            IconButton(onClick = { confirmDeleteProfile = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Profili sil")
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (profile != null) {
                ExtendedFloatingActionButton(
                    onClick = onAddEvent,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Kayıt ekle") },
                )
            }
        },
    ) { padding ->
        if (profile == null) {
            if (!state.loading) {
                Text("Profil bulunamadı.", modifier = Modifier.padding(padding).padding(24.dp))
            }
            return@Scaffold
        }
        val analysis = state.analysis
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(profile, analysis) }
            if (analysis != null) {
                item { StanceCard(analysis) }
                if (!analysis.isEmpty) item { Metrics(analysis) }
            }
            if (profile.notes.isNotBlank()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Notlar", style = MaterialTheme.typography.titleSmall)
                            Text(profile.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text(
                    "Kayıtlar (${state.events.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.events.isEmpty()) {
                item {
                    Text(
                        "Henüz kayıt yok. Aranızda geçen bir iyiliği ya da kırgınlığı ekle.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.events, key = { it.id }) { e ->
                EventRow(e, onDelete = { eventToDelete = e })
            }
        }
    }

    if (editing && profile != null) {
        ProfileEditDialog(
            initial = profile,
            onDismiss = { editing = false },
            onSave = {
                onUpdateProfile(it)
                editing = false
            },
        )
    }

    eventToDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Kayıt silinsin mi?") },
            text = { Text("\"${e.title}\" kaydı kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteEvent(e)
                    eventToDelete = null
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { eventToDelete = null }) { Text("Vazgeç") } },
        )
    }

    if (confirmDeleteProfile) {
        AlertDialog(
            onDismissRequest = { confirmDeleteProfile = false },
            title = { Text("Profil silinsin mi?") },
            text = { Text("Bu kişi ve bütün kayıtları kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteProfile = false
                    onDeleteProfile()
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteProfile = false }) { Text("Vazgeç") } },
        )
    }
}

@Composable
private fun Header(profile: ProfileEntity, analysis: RelationshipAnalysis?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(profile.displayName, profile.photoUri, size = 64.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(profile.relation.label)
                    append(" · ")
                    append("★".repeat(profile.importance))
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            profile.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (profile.contactMissing) {
                Text(
                    "Rehberden silinmiş; geçmiş korunuyor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (analysis != null && !analysis.isEmpty) {
                Text(
                    netSentence(analysis.net),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = LedgerColors.forNet(analysis.net),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun Metrics(a: RelationshipAnalysis) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Alacak: +${a.credit}", color = LedgerColors.credit, fontWeight = FontWeight.SemiBold)
                Text("Borç: −${a.debt}", color = LedgerColors.debt, fontWeight = FontWeight.SemiBold)
                Text("Net: ${formatNet(a.net)}", color = LedgerColors.forNet(a.net), fontWeight = FontWeight.SemiBold)
            }
            LabeledBar(
                label = "Güven (karşılık verme eğilimi)",
                value = a.trust.toFloat(),
                color = MaterialTheme.colorScheme.primary,
                trailing = "%${(a.trust * 100).roundToInt()}",
            )
            LabeledBar(
                label = "İlişki sağlığı",
                value = a.health / 100f,
                color = LedgerColors.forTone(a.stance.tone),
                trailing = "${a.health}/100",
            )
        }
    }
}

@Composable
private fun EventRow(e: EventEntity, onDelete: () -> Unit) {
    val kind = e.eventKind
    val sign = kind.ledgerSign
    val color = if (sign > 0) LedgerColors.credit else LedgerColors.debt
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${kind.shortLabel} · ${formatDate(e.occurredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (e.note.isNotBlank()) {
                    Text(e.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Text(
                (if (sign > 0) "+" else "−") + e.points,
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Kaydı sil", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
