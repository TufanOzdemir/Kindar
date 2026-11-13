package com.tufanozdemir.kindar.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tufanozdemir.kindar.domain.RelationshipAnalysis
import com.tufanozdemir.kindar.ui.theme.LedgerColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TR = Locale("tr", "TR")

fun formatDate(millis: Long): String = SimpleDateFormat("d MMMM yyyy", TR).format(Date(millis))

fun formatNet(net: Int): String = when {
    net > 0 -> "+$net"
    net < 0 -> "−${-net}"
    else -> "0"
}

fun netSentence(net: Int): String = when {
    net > 0 -> "$net puan alacaklısın"
    net < 0 -> "${-net} puan borçlusun"
    else -> "Denge"
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase(TR) }
        .ifEmpty { "?" }

@Composable
fun Avatar(name: String, photoUri: String?, size: Dp = 44.dp) {
    val context = LocalContext.current
    val bitmap: Bitmap? by produceState<Bitmap?>(initialValue = null, photoUri) {
        value = if (photoUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))?.use(BitmapFactory::decodeStream)
                }.getOrNull()
            }
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = if (size > 50.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
fun NetBadge(net: Int, modifier: Modifier = Modifier) {
    val color = LedgerColors.forNet(net)
    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(50),
        modifier = modifier,
    ) {
        Text(
            formatNet(net),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun LabeledBar(label: String, value: Float, color: Color, trailing: String) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(trailing, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
        )
    }
}

/** Öneri kartı: duruşun başlığı ve somut öneri metni. */
@Composable
fun StanceCard(analysis: RelationshipAnalysis, modifier: Modifier = Modifier) {
    val color = LedgerColors.forTone(analysis.stance.tone)
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Önerilen hamle: ${analysis.stance.title}",
                color = color,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                analysis.advice,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
