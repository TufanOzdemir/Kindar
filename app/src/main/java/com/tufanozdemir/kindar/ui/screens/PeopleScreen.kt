package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.data.relation
import com.tufanozdemir.kindar.domain.Stance
import com.tufanozdemir.kindar.ui.ProfileSummary
import com.tufanozdemir.kindar.ui.components.Avatar
import com.tufanozdemir.kindar.ui.components.NetBadge
import java.util.Locale

enum class PeopleFilter(val label: String) {
    WITH_RECORDS("Kayıtlı"),
    ALL("Tümü"),
    I_OWE("Borçlu olduklarım"),
    OWED_TO_ME("Alacaklı olduklarım"),
    IMPORTANT("Önemliler"),
}

@Composable
fun PeopleScreen(
    summaries: List<ProfileSummary>,
    onOpenProfile: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(PeopleFilter.ALL) }
    val tr = remember { Locale("tr", "TR") }

    val visible = remember(summaries, query, filter) {
        val q = query.trim().lowercase(tr)
        summaries
            .asSequence()
            .filter { q.isEmpty() || it.profile.displayName.lowercase(tr).contains(q) || (it.profile.phone ?: "").contains(q) }
            .filter {
                when (filter) {
                    PeopleFilter.ALL -> true
                    PeopleFilter.WITH_RECORDS -> !it.analysis.isEmpty
                    PeopleFilter.I_OWE -> it.analysis.net < 0
                    PeopleFilter.OWED_TO_ME -> it.analysis.net > 0
                    PeopleFilter.IMPORTANT -> it.profile.importance >= 4
                }
            }
            .sortedWith(
                when (filter) {
                    PeopleFilter.I_OWE -> compareBy<ProfileSummary> { it.analysis.net }
                    PeopleFilter.OWED_TO_ME -> compareByDescending<ProfileSummary> { it.analysis.net }
                    PeopleFilter.WITH_RECORDS -> compareByDescending<ProfileSummary> { it.analysis.lastEventMillis ?: 0L }
                    else -> compareBy<ProfileSummary> { it.profile.displayName.lowercase(tr) }
                },
            )
            .toList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Kişi ara") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PeopleFilter.entries.forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        if (visible.isEmpty()) {
            Text(
                if (summaries.isEmpty()) {
                    "Henüz kişi yok. Rehberi bağla ya da sağ alttaki + ile kişi ekle."
                } else {
                    "Bu filtreye uyan kişi yok."
                },
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 88.dp),
        ) {
            items(visible, key = { it.profile.id }) { s ->
                PersonRow(s, onClick = { onOpenProfile(s.profile.id) })
                HorizontalDivider(Modifier.padding(start = 72.dp))
            }
        }
    }
}

@Composable
private fun PersonRow(s: ProfileSummary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(s.profile.displayName, s.profile.photoUri)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.profile.displayName, style = MaterialTheme.typography.bodyLarge)
            val sub = buildString {
                append(s.profile.relation.label)
                if (!s.analysis.isEmpty && s.analysis.stance != Stance.HEALTHY) {
                    append(" · ").append(s.analysis.stance.title)
                }
                if (s.profile.contactMissing) append(" · rehberde yok")
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!s.analysis.isEmpty) NetBadge(s.analysis.net)
    }
}
