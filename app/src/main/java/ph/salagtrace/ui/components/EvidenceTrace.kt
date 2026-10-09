package ph.salagtrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import ph.salagtrace.ui.theme.PlexMono
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import ph.salagtrace.data.ScannedMessageEntity

@Composable
fun EvidenceTrace(message: ScannedMessageEntity, modifier: Modifier = Modifier) {
    val evidence = message.evidence(); val rules = message.rules().signals
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val offsets = IntArray(message.maskedBody.length)
    val text = buildAnnotatedString {
        message.maskedBody.forEachIndexed { index, c ->
            offsets[index] = length
            val ai = evidence.any { index in it.span.start until it.span.end }
            val rule = rules.any { index in it.span.start until it.span.end }
            val masked = message.masks().any { index in it.start until it.end }
            withStyle(SpanStyle(
                background = when { ai -> accent.copy(alpha = 0.18f); masked -> muted.copy(alpha = 0.18f); else -> Color.Transparent },
                textDecoration = if (ai) TextDecoration.Underline else null,
            )) { append(c) }
            evidence.forEachIndexed { number, item ->
                if (index + 1 == item.span.end) withStyle(SpanStyle(color = accent, baselineShift = BaselineShift.Superscript)) { append(listOf("①", "②", "③").getOrElse(number) { "•" }) }
            }
            if (!ai && rules.any { index + 1 == it.span.end }) withStyle(SpanStyle(color = muted, baselineShift = BaselineShift.Superscript)) { append("R") }
        }
    }
    // Text intentionally has no link annotations, ClickableText, or URI handlers.
    var layout by remember(message.id, message.maskedBody, message.evidenceJson) { mutableStateOf<TextLayoutResult?>(null) }
    Text(text, modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp).drawBehind {
        val measured = layout ?: return@drawBehind
        rules.forEach { signal ->
            val boxes = (signal.span.start until signal.span.end).filter { it in offsets.indices }.map { measured.getBoundingBox(offsets[it]) }
            boxes.groupBy { it.top }.values.forEach { line ->
                val left = line.minOf { it.left }; val right = line.maxOf { it.right }
                val top = line.minOf { it.top }; val bottom = line.maxOf { it.bottom }
                drawRoundRect(muted.copy(alpha = .8f), androidx.compose.ui.geometry.Offset(left,top), androidx.compose.ui.geometry.Size((right-left).coerceAtLeast(1f),bottom-top), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(),3.dp.toPx()))))
            }
        }
    }, fontFamily = PlexMono, style = MaterialTheme.typography.bodyLarge, onTextLayout = { layout = it })
}
