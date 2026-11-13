package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.domain.SelfReport
import com.tufanozdemir.kindar.ui.components.LabeledBar
import com.tufanozdemir.kindar.ui.components.StatCard
import com.tufanozdemir.kindar.ui.theme.LedgerColors
import kotlin.math.roundToInt

@Composable
fun SelfScreen(
    report: SelfReport?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (report == null) return@Column

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("İlişki tarzın", style = MaterialTheme.typography.labelLarge)
                Text(
                    report.giverStyle.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(report.giverStyle.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Açık borç", "${report.openDebts}", LedgerColors.debt, Modifier.weight(1f))
            StatCard("Onarılacak", "${report.pendingRepairs}", LedgerColors.debt, Modifier.weight(1f))
            StatCard("Uykuda", "${report.dormantImportant}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val ratio = report.givingRatio
                if (ratio != null) {
                    // 2.0 ve üstü çubuğu doldurur; 1.0 tam denge.
                    LabeledBar(
                        label = "Verme / alma oranı (son 1 yıl)",
                        value = (ratio / 2.0).toFloat(),
                        color = MaterialTheme.colorScheme.primary,
                        trailing = "%.2f".format(ratio),
                    )
                }
                val rel = report.myReliability
                if (rel != null) {
                    LabeledBar(
                        label = "Sözünü tutma oranın",
                        value = rel.toFloat(),
                        color = LedgerColors.credit,
                        trailing = "%${(rel * 100).roundToInt()}",
                    )
                }
                if (ratio == null && rel == null) {
                    Text("Oranlar için biraz daha kayıt gerekiyor.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("Sana önerilerim", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        report.tips.forEach { tip ->
            Card(Modifier.fillMaxWidth()) {
                Text(tip, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Yedekleme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Kindar sunucu kullanmaz, bütün veriler bu telefonda durur. Telefon değiştirmeden önce yedek al.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Text("Dışa aktar") }
            OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Geri yükle") }
        }
    }
}
