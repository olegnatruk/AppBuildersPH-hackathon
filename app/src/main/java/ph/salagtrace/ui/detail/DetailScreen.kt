package ph.salagtrace.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ph.salagtrace.R
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.ui.AppViewModel
import ph.salagtrace.ui.components.*
import ph.salagtrace.ui.theme.*
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable fun DetailScreen(vm: AppViewModel, m: ScannedMessageEntity?, language: AppLanguage, onDeleted: () -> Unit, openSettings: () -> Unit) {
    if(m == null) { Text(text(R.string.record_missing), Modifier.padding(24.dp)); return }
    var deleting by remember { mutableStateOf(false) }; var copy by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val pending = m.state in setOf(AnalysisState.QUEUED_AI,AnalysisState.ANALYZING)
    LaunchedEffect(pending) { while(pending) { now = System.currentTimeMillis(); delay(1000) } }
    val context = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { BoardCard {
            VerdictBadge(m.verdict, true)
            Text(if(m.isAiResult()) text(R.string.confidence,text(when(m.confidence) { Confidence.HIGH -> R.string.high; Confidence.MEDIUM -> R.string.medium; Confidence.LOW -> R.string.low })) else text(R.string.rule_confidence), style = MaterialTheme.typography.bodySmall)
            Text(categoryText(m.category), style = MaterialTheme.typography.titleMedium)
            Tag(if(m.isAiResult()) text(R.string.on_device_ai) + " · " + modelLabel(m.modelName!!) + " · " + decimal((m.latencyMs ?: 0)/1000.0) + " s · offline" else text(R.string.rule_check) + " · " + stateText(m))
            if(pending) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(text(if(m.state == AnalysisState.QUEUED_AI) R.string.queued else R.string.analyzing) + " · ${((now - (m.analysisStartedAt ?: m.receivedAt))/1000).coerceAtLeast(0)} s", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { vm.cancel(m.id) }) { Text(text(R.string.cancel)) }
            }
            if(m.error?.startsWith("Analysis canceled") == true) Text(text(R.string.canceled), style = MaterialTheme.typography.bodySmall)
            if("AI_RETRIED" in m.notes()) Text(text(R.string.retried), style = MaterialTheme.typography.bodySmall)
            if(m.state == AnalysisState.AI_UNAVAILABLE) OutlinedButton(onClick = openSettings) { Text(text(R.string.install_model)) }
            if(m.state in setOf(AnalysisState.AI_TIMEOUT,AnalysisState.AI_FAILED)) Text(stateText(m), color = MaterialTheme.colorScheme.error)
        } }
        if(m.disagreement) item { BoardCard { Text(text(R.string.disagreement), color = MaterialTheme.colorScheme.primary) } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(R.string.trace)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag(m.source.name); Text(m.sender, style = MaterialTheme.typography.titleMedium) }
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(m.receivedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            EvidenceTrace(m, Modifier.fillMaxWidth())
            Text(text(R.string.trace_legend), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if(m.maskedBody.length > 800 && m.isAiResult()) Text(text(R.string.note_truncated), style = MaterialTheme.typography.bodySmall)
        } }
        if(m.isAiResult()) item { SectionTitle(R.string.evidence) }
        items(m.evidence().withIndex().toList()) { (i,e) -> BoardCard {
            Text("${listOf("①","②","③").getOrElse(i){"•"}}  ${e.quote}", fontFamily = PlexMono, color = MaterialTheme.colorScheme.primary)
            Tag(text(if(e.verification == Verification.VERIFIED) R.string.verified else R.string.partial))
            Text(text(R.string.reason_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(e.reason)
        } }
        if(m.isAiResult() && m.evidence().isEmpty()) item { Text(text(R.string.no_evidence)) }
        if(m.droppedEvidence > 0) item { Text(text(R.string.dropped,m.droppedEvidence), style = MaterialTheme.typography.bodySmall) }
        item { BoardCard {
            SectionTitle(R.string.rule_signals)
            val labels = m.rules().signals.map { ScamRules.label(it.id, language) }.distinct()
            if(labels.isEmpty()) Text(text(R.string.no_signals), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { labels.forEach { Tag(it) } }
        } }
        m.explanation?.let { explanation -> item { BoardCard {
            SectionTitle(R.string.explanation); Text(explanation)
            Text(text(R.string.explanation_language,if(m.explanationLang == "FILIPINO") "Filipino" else "English"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        item { BoardCard { SectionTitle(R.string.actions); Recommendations.forCategory(m.category,language).forEach { Text("• $it") } } }
        if(m.rules().urls.isNotEmpty()) item { SectionTitle(R.string.links) }
        items(m.rules().urls) { link -> BoardCard {
            Text(link.domain, style = MaterialTheme.typography.titleLarge)
            Text(link.url, fontFamily = PlexMono, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { copy = link.url }) { Text(text(R.string.copy_link)) }
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { vm.rerun(m.id) }, enabled = !pending, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.retry)) }
            TextButton(onClick = { deleting = true }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.delete_record), color = MaterialTheme.colorScheme.error) }
            Text(text(R.string.disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
    }
    if(deleting) ConfirmDialog(R.string.delete_title,R.string.delete_body,{ deleting = false },{ vm.delete(m.id); deleting = false; onDeleted() })
    copy?.let { url -> ConfirmDialog(R.string.copy_link,R.string.copy_warning,{ copy = null },{ context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Link",url)); copy = null }, R.string.copy) }
}
