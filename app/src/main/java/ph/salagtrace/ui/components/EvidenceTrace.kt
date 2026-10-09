package ph.salagtrace.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val text = buildAnnotatedString {
        message.maskedBody.forEachIndexed { index, c ->
            val ai = evidence.any { index in it.span.start until it.span.end }
            val rule = rules.any { index in it.span.start until it.span.end }
            val masked = message.masks().any { index in it.start until it.end }
            withStyle(SpanStyle(
                background = when { ai -> Color(0xFFF5B83D).copy(alpha = 0.22f); masked -> Color(0xFF8E9BB3).copy(alpha = 0.2f); else -> Color.Transparent },
                textDecoration = if (ai || rule) TextDecoration.Underline else null,
            )) { append(c) }
            evidence.forEachIndexed { number, item ->
                if (index + 1 == item.span.end) withStyle(SpanStyle(color = Color(0xFFB7791F), baselineShift = BaselineShift.Superscript)) { append(listOf("①", "②", "③").getOrElse(number) { "•" }) }
            }
            if (!ai && rules.any { index + 1 == it.span.end }) withStyle(SpanStyle(color = Color(0xFF8E9BB3), baselineShift = BaselineShift.Superscript)) { append("R") }
        }
    }
    // Text intentionally has no link annotations, ClickableText, or URI handlers.
    Text(text, modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge)
}
