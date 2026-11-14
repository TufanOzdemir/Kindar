package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.ui.OverviewState
import com.tufanozdemir.kindar.ui.ProfileSummary
import com.tufanozdemir.kindar.ui.components.Avatar
import com.tufanozdemir.kindar.ui.components.NetBadge
import com.tufanozdemir.kindar.ui.components.StatCard
import com.tufanozdemir.kindar.ui.theme.LedgerColors

@Composable
fun HomeScreen(
    state: OverviewState,
    hasContactsPermission: Boolean,
    onRequestContacts: () -> Unit,
    onOpenProfile: (Long) -> Unit,
    contentPadding: PaddingValues,
) {
    val report = state.report
    val actions = state.actions.take(30)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (report != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Toplam alacak", "+${report.totalCredit}", LedgerColors.credit, Modifier.weight(1f))
                    StatCard("Toplam borç", "−${report.totalDebt}", LedgerColors.debt, Modifier.weight(1f))
                    StatCard(
                        "Sağlıklı",
                        "${report.healthyRelations}/${report.activeRelations}",
                        MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f),
                    )
                }
            }
        }

        if (!hasContactsPermission) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Rehberini bağla", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Rehberdeki kişiler otomatik olarak profil olur. Veriler yalnızca bu cihazda saklanır, " +
                                "hiçbir sunucuya gönderilmez.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        Button(onClick = onRequestContacts) { Text("Rehbere erişim ver") }
                    }
                }
            }
        }

        item {
            Text(
                "Bugünün hamleleri",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                "Cömert karşılıklılık modeline göre en çok fark yaratacak adımlar, en acilden başlayarak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (!state.loading && actions.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "Şu an acil bir hamle yok. Kişiler sekmesinden bir kişiye kayıt ekleyerek başla; " +
                            "ya da önemli bir ilişkine karşılıksız küçük bir iyilik yap.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        items(actions, key = { it.profile.id }) { s ->
            ActionCard(s, onClick = { onOpenProfile(s.profile.id) })
        }
    }
}

@Composable
private fun ActionCard(s: ProfileSummary, onClick: () -> Unit) {
    val toneColor = LedgerColors.forTone(s.analysis.stance.tone)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(s.profile.displayName, s.profile.photoUri)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.profile.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        s.analysis.stance.title,
                        color = toneColor,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                if (!s.analysis.isEmpty) NetBadge(s.analysis.net)
            }
            Text(
                s.analysis.advice,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}
