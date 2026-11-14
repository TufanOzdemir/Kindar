package com.tufanozdemir.kindar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.data.ProfileEntity
import com.tufanozdemir.kindar.domain.RelationType

private val importanceHints = mapOf(
    1 to "Önemsiz: nadiren karşılaşırız",
    2 to "Az önemli",
    3 to "Orta: düzenli görüşürüz",
    4 to "Önemli: hayatımda yeri büyük",
    5 to "Çok önemli: uzun vadede vazgeçilmez",
)

@Composable
fun RelationTypePicker(selected: RelationType, onSelect: (RelationType) -> Unit) {
    Column {
        Text("İlişki türü", style = MaterialTheme.typography.titleSmall)
        RelationType.entries.forEach { t ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = t == selected, onClick = { onSelect(t) }, role = Role.RadioButton)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = t == selected, onClick = null)
                Text(t.label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            if (selected.communal) {
                "Yakın ilişkilerde puanlar yumuşak tutulur; öneriler ihtiyaç odaklıdır."
            } else {
                "Bu ilişkide karşılıksız kalan verme payın en fazla yüzde ${(selected.generosityTolerance * 100).toInt()}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ImportancePicker(value: Int, onChange: (Int) -> Unit) {
    Column {
        Text("Gelecekteki önemi", style = MaterialTheme.typography.titleSmall)
        Row {
            (1..5).forEach { i ->
                IconButton(onClick = { onChange(i) }) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "$i yıldız",
                        tint = if (i <= value) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
        Text(
            importanceHints[value].orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ProfileEditDialog(
    initial: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (ProfileEntity) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial.displayName) }
    var type by rememberSaveable { mutableStateOf(RelationType.fromName(initial.relationType)) }
    var importance by rememberSaveable { mutableIntStateOf(initial.importance) }
    var notes by rememberSaveable { mutableStateOf(initial.notes) }
    val nameEditable = initial.contactLookupKey == null || initial.contactMissing

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profili düzenle") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (nameEditable) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("İsim") }, singleLine = true)
                } else {
                    Text("İsim rehberden gelir: $name", style = MaterialTheme.typography.bodySmall)
                }
                RelationTypePicker(type) { type = it }
                ImportancePicker(importance) { importance = it }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notlar") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            displayName = name.trim(),
                            relationType = type.name,
                            importance = importance,
                            notes = notes.trim(),
                        ),
                    )
                },
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProfileScreen(
    onBack: () -> Unit,
    onSave: (name: String, phone: String?, type: RelationType, importance: Int, notes: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(RelationType.FRIEND) }
    var importance by rememberSaveable { mutableIntStateOf(3) }
    var notes by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yeni kişi") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Rehberdeki kişiler otomatik eklenir. Rehberde olmayan biri için buradan profil oluştur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("İsim") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefon (isteğe bağlı)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            RelationTypePicker(type) { type = it }
            ImportancePicker(importance) { importance = it }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notlar") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave(name, phone.ifBlank { null }, type, importance, notes) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Kaydet") }
        }
    }
}
