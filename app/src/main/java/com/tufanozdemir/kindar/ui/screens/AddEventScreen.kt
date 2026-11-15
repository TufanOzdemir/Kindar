package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.ui.components.bringIntoViewOnFocus
import com.tufanozdemir.kindar.domain.EventKind
import com.tufanozdemir.kindar.ui.components.formatDate
import com.tufanozdemir.kindar.ui.theme.LedgerColors
import kotlin.math.roundToInt

/** Puan rehberi: kullanıcı tutarlı puan versin diye her aralık için örnekler. */
private fun pointsGuide(points: Int): String = when (points) {
    in 1..2 -> "Küçük: bir kahve ısmarlamak, küçük bir yardım, kısa bir gecikme."
    in 3..4 -> "Orta: bir akşamını ayırmak, işini kolaylaştırmak, unutulan bir söz."
    in 5..6 -> "Belirgin: taşınmaya yardım, önemli bir tavsiye, önemli bir günü kaçırmak."
    in 7..8 -> "Büyük: zor bir dönemde yanında olmak, ciddi bir maddi destek, güveni sarsan bir olay."
    else -> "Çok büyük: hayatını değiştiren bir destek ya da ihanet."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    personName: String,
    onBack: () -> Unit,
    onSave: (kind: EventKind, points: Int, title: String, note: String, occurredAt: Long) -> Unit,
) {
    var kind by rememberSaveable { mutableStateOf(EventKind.I_GAVE) }
    var points by rememberSaveable { mutableFloatStateOf(3f) }
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val p = points.roundToInt()
    val color = if (kind.ledgerSign > 0) LedgerColors.credit else LedgerColors.debt

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$personName için kayıt") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Klavye açıldığında içerik onun üstünde kalsın ve kaydırılabilsin.
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("Ne oldu?", style = MaterialTheme.typography.titleSmall)
                EventKind.entries.forEach { k ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = k == kind, onClick = { kind = k }, role = Role.RadioButton)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = k == kind, onClick = null)
                        Text(k.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Büyüklük", style = MaterialTheme.typography.titleSmall)
                    Text("$p puan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
                }
                Slider(value = points, onValueChange = { points = it }, valueRange = 1f..10f, steps = 8)
                Text(pointsGuide(p), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (kind.ledgerSign > 0) "Bu kayıt alacağını $p puan arttırır." else "Bu kayıt borcunu $p puan arttırır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Başlık") },
                placeholder = { Text("Örn. Taşınmada yardım etti") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Ayrıntılar") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().bringIntoViewOnFocus(),
            )
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Text(formatDate(date), modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = { onSave(kind, p, title, note, date) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Kaydet") }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = it }
                    showDatePicker = false
                }) { Text("Tamam") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Vazgeç") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
