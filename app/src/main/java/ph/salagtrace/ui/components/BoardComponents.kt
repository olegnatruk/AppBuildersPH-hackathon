package ph.salagtrace.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.data.ScannedMessageEntity
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*
import ph.salagtrace.ui.theme.*
import java.io.File
import java.util.Locale

@Composable fun text(@StringRes id: Int, vararg args: Any): String = stringResource(id, *args)
@Composable fun SectionTitle(@StringRes id: Int) { Text(text(id), style = MaterialTheme.typography.titleLarge) }
@Composable fun BoardCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun Tag(label: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(label, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}
@Composable fun verdictText(v: Verdict) = text(when(v) {
    Verdict.LIKELY_SCAM -> R.string.likely_scam; Verdict.SUSPICIOUS -> R.string.suspicious
    Verdict.UNCERTAIN -> R.string.uncertain; Verdict.LIKELY_SAFE -> R.string.likely_safe
})
@Composable fun VerdictBadge(v: Verdict, large: Boolean = false) {
    val color = riskColor(v, LocalDarkTheme.current)
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = .12f), border = BorderStroke(1.dp, color)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = if (large) 14.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(when(v) { Verdict.LIKELY_SCAM -> Icons.Outlined.GppBad; Verdict.SUSPICIOUS -> Icons.Outlined.WarningAmber; Verdict.UNCERTAIN -> Icons.Outlined.HelpOutline; Verdict.LIKELY_SAFE -> Icons.Outlined.VerifiedUser }, null, tint = color)
            Text(verdictText(v), color = color, style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge)
        }
    }
}
@Composable fun stateText(m: ScannedMessageEntity) = text(when(m.state) {
    AnalysisState.SCREENED -> R.string.rule_check; AnalysisState.QUEUED_AI -> R.string.queued
    AnalysisState.ANALYZING -> R.string.analyzing; AnalysisState.DONE -> R.string.on_device_ai
    AnalysisState.AI_UNAVAILABLE -> R.string.ai_unavailable; AnalysisState.AI_TIMEOUT -> R.string.ai_timeout; AnalysisState.AI_FAILED -> R.string.ai_failed
})
@Composable fun modelState(file: File?, engine: EngineState): String = text(when {
    file == null -> R.string.missing; engine is EngineState.Loading -> R.string.loading
    engine is EngineState.Busy -> R.string.analyzing; engine is EngineState.Ready -> R.string.ready
    engine is EngineState.Error -> R.string.model_error; else -> R.string.unloaded
})
fun modelLabel(name: String) = when {
    name.contains("qwen2.5-1.5b", true) -> "Qwen2.5 1.5B · Q4_K_M"
    name.contains("qwen2.5-0.5b", true) -> "Qwen2.5 0.5B · Q4_K_M"
    name.contains("gemma-3-1b", true) -> "Gemma 3 1B · Q4_K_M"
    else -> name
}
@Composable fun categoryText(c: Category): String = text(when(c) {
    Category.OTP_CREDENTIAL_THEFT -> R.string.category_otp_credential_theft
    Category.PHISHING_LINK -> R.string.category_phishing_link
    Category.ACCOUNT_SUSPENSION -> R.string.category_account_suspension
    Category.FAKE_PRIZE -> R.string.category_fake_prize
    Category.BANK_EWALLET_IMPERSONATION -> R.string.category_bank_ewallet_impersonation
    Category.GOVERNMENT_IMPERSONATION -> R.string.category_government_impersonation
    Category.FAMILY_IMPERSONATION -> R.string.category_family_impersonation
    Category.JOB_INVESTMENT -> R.string.category_job_investment
    Category.LOAN_DEBT -> R.string.category_loan_debt
    Category.DELIVERY_PARCEL -> R.string.category_delivery_parcel
    Category.OTHER_SCAM -> R.string.category_other_scam
    Category.NONE -> R.string.category_none
})
@Composable fun StatusRow(icon: ImageVector, label: String, value: String, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.titleSmall); Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        action?.invoke()
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable fun MessageRow(m: ScannedMessageEntity, onClick: () -> Unit, onDelete: () -> Unit = {}) {
    BoardCard(Modifier.combinedClickable(onClick = onClick, onLongClick = onDelete)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(m.sender, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (m.source != Source.SMS) Tag(m.source.name)
        }
        VerdictBadge(m.verdict)
        Text(m.maskedBody, fontFamily = PlexMono, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stateText(m), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(m.receivedAt)), style = MaterialTheme.typography.labelSmall, fontFamily = PlexMono)
        }
        if (m.state in setOf(AnalysisState.QUEUED_AI, AnalysisState.ANALYZING)) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
@Composable fun ConfirmDialog(title: Int, body: Int, onDismiss: () -> Unit, onConfirm: () -> Unit, confirm: Int = R.string.delete) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(text(title)) }, text = { Text(text(body)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text(confirm)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(text(R.string.cancel)) } })
}
fun decimal(value: Double) = String.format(Locale.US, "%.1f", value)
