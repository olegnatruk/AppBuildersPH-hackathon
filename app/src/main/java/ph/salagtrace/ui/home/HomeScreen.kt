package ph.salagtrace.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*
import ph.salagtrace.ui.*
import ph.salagtrace.ui.components.*
import java.io.File
import java.util.Calendar

@Composable fun HomeScreen(vm: AppViewModel, settings: AppSettings, permissions: PermissionState, messages: List<ScannedMessageEntity>, file: File?, engine: EngineState, askSms: () -> Unit, askAlerts: () -> Unit, openSettings: () -> Unit, openCheck: () -> Unit, openDetail: (Long) -> Unit) {
    val monitoring = settings.monitoringEnabled && permissions.sms
    val fullyProtected = monitoring && file != null && engine !is EngineState.Error
    val today = messages.filter { it.receivedAt >= Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }.timeInMillis }
    val flagged = messages.filter { it.verdict in setOf(Verdict.LIKELY_SCAM, Verdict.SUSPICIOUS) }.take(5)
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text(text(R.string.tagline), style = MaterialTheme.typography.headlineLarge)
            Text(text(R.string.welcome_body), Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { BoardCard {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(if(fullyProtected) Icons.Outlined.VerifiedUser else Icons.Outlined.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Text(text(if(fullyProtected) R.string.status_protected else R.string.not_protected), style = MaterialTheme.typography.titleLarge)
            }
            Text(text(if(!monitoring) R.string.monitoring_off else if(engine is EngineState.Error) R.string.model_error else if(file == null) R.string.rule_protection else R.string.protection_body), style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            StatusRow(Icons.Outlined.Sms, text(R.string.monitoring), text(if(monitoring) R.string.on else R.string.off)) { Switch(monitoring, { if(it && !permissions.sms) askSms() else vm.setMonitoring(it) }) }
            StatusRow(Icons.Outlined.Key, text(R.string.sms_permission), text(if(permissions.sms) R.string.granted else R.string.denied)) { if(!permissions.sms) TextButton(onClick = askSms) { Text(text(R.string.fix)) } }
            StatusRow(Icons.Outlined.Memory, text(R.string.model), file?.let { modelLabel(it.name) + "\n" + modelState(it, engine) } ?: text(R.string.missing)) { if(file == null || engine is EngineState.Error) TextButton(onClick = openSettings) { Text(text(R.string.fix)) } }
            StatusRow(Icons.Outlined.Notifications, text(R.string.notifications), text(if(permissions.notifications) R.string.on else R.string.off)) { if(!permissions.notifications) TextButton(onClick = askAlerts) { Text(text(R.string.fix)) } }
        } }
        item { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(R.string.today)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(R.string.scanned to today.size, R.string.flagged to today.count { it.verdict in setOf(Verdict.LIKELY_SCAM,Verdict.SUSPICIOUS) }, R.string.ai_runs to today.count { it.isAiResult() }).forEach { (label,count) ->
                    Surface(Modifier.weight(1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(12.dp)) { Text(count.toString(), style = MaterialTheme.typography.headlineMedium); Text(text(label), style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        } }
        item { SectionTitle(R.string.recent_flagged) }
        if(flagged.isEmpty()) item { BoardCard { Text(text(R.string.empty_flagged)) } }
        items(flagged, key = { it.id }) { MessageRow(it, { openDetail(it.id) }) }
        item { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { vm.sample(openDetail) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Science, null); Spacer(Modifier.width(8.dp)); Text(text(R.string.sample)) }
            OutlinedButton(onClick = openCheck, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.check_message)) }
            Tag(text(R.string.offline))
        } }
    }
}
