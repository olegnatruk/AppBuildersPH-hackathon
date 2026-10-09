package ph.salagtrace.ui.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.ui.*
import ph.salagtrace.llm.ImportState
import ph.salagtrace.ui.components.EvidenceTrace
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalagNavHost(vm: AppViewModel, pendingId: Long?, onHandled: () -> Unit, onPermissions: () -> Unit) {
    val nav = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val model by vm.activeModel.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onPermissions() }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    LaunchedEffect(pendingId) { if (pendingId != null) { nav.navigate("detail/$pendingId"); onHandled() } }
    Scaffold(topBar = { TopAppBar(title = { Text("SALAG TRACE") }) }) { padding ->
        NavHost(nav, startDestination = "messages", modifier = Modifier.padding(padding)) {
            composable("messages") {
                var consent by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Suri bago pindot. · Offline · No internet permission")
                    if (settings?.consentAt == 0L) {
                        Text("Only new SMS. Analysis stays on this phone. Secrets are masked before saving. You can disable monitoring or delete records. Never blocks, deletes or replies to carrier SMS.")
                        Row { Checkbox(consent, { consent = it }); Text("I understand and consent", Modifier.padding(top = 12.dp)) }
                        Button(enabled = consent, onClick = { vm.consent() }) { Text("Save consent") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { permissionLauncher.launch(if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS) else arrayOf(Manifest.permission.RECEIVE_SMS)) }) { Text("Allow SMS & alerts") }
                        Switch(settings?.monitoringEnabled == true, { vm.setMonitoring(it) }, enabled = (settings?.consentAt ?: 0) > 0 && permissions.sms)
                    }
                    Text("Model: ${model?.name ?: "Missing — rule check only"}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { importer.launch(arrayOf("*/*")) }) { Text("Import GGUF") }
                        TextButton(onClick = { vm.rescanModels() }) { Text("Rescan") }
                    }
                    when (val state = importing) {
                        is ImportState.Copying -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Importing ${state.bytes / 1_000_000} MB") }
                        is ImportState.Error -> Text(state.reason)
                        is ImportState.Done -> Text("Imported ${state.name}")
                        else -> Unit
                    }
                    Button(onClick = { vm.sample { nav.navigate("detail/$it") } }) { Text("Try a sample scam (SAMPLE)") }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages, key = { it.id }) { m ->
                            Card(Modifier.fillMaxWidth().clickable { nav.navigate("detail/${m.id}") }) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("${m.verdict.name.replace('_', ' ')} · ${m.sender}")
                                    Text(m.maskedBody.take(90), maxLines = 2)
                                    Text("${m.state} · ${m.source}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                val row by remember(id) { vm.message(id) }.collectAsStateWithLifecycle(null)
                row?.let { m ->
                    Column(Modifier.fillMaxSize()) {
                        TextButton(onClick = { nav.popBackStack() }) { Text("← Messages") }
                        LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Text(m.verdict.name.replace('_', ' '), style = MaterialTheme.typography.headlineMedium)
                                Text("${m.confidence} confidence · ${m.category.name.replace('_', ' ')}")
                                Text(if (m.isAiResult()) "On-device AI · ${m.modelName} · ${(m.latencyMs ?: 0) / 1000.0} s · offline" else "Rule check · ${m.state}")
                                m.error?.let { Text(it) }
                                if (m.disagreement) Text("AI and rule check disagree. Be cautious.")
                                if (m.state == AnalysisState.ANALYZING || m.state == AnalysisState.QUEUED_AI) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                    Text("${m.state} · analyzing on this phone")
                                    TextButton(onClick = { vm.cancel(id) }) { Text("Cancel") }
                                }
                            }
                            item { Text("${m.source} · ${m.sender} · ${DateFormat.getDateTimeInstance().format(Date(m.receivedAt))}"); EvidenceTrace(m, Modifier.fillMaxWidth()) }
                            items(m.evidence()) { e -> Text("${m.evidence().indexOf(e) + 1}. ${e.quote}\n${e.reason} · ${e.verification}") }
                            item {
                                if (m.droppedEvidence > 0) Text("${m.droppedEvidence} AI claims removed: quote not found in this message.")
                                if ("NO_VERIFIED_EVIDENCE" in m.notes()) Text("No verified AI evidence. Confidence lowered.")
                                Text("Rule signals: " + m.rules().signals.map { ScamRules.label(it.id, AppLanguage.ENGLISH) }.distinct().joinToString(", "))
                            }
                            m.explanation?.let { explanation -> item { Text("AI explanation — may be wrong", style = MaterialTheme.typography.titleMedium); Text(explanation) } }
                            item { Text("Recommended actions", style = MaterialTheme.typography.titleMedium); Recommendations.forCategory(m.category, settings?.language ?: AppLanguage.FILIPINO).forEach { Text("• $it") } }
                            items(m.rules().urls) { link -> Text("${link.domain}\n${link.url}") }
                            item {
                                Row { OutlinedButton(onClick = { vm.rerun(id) }, enabled = m.state !in setOf(AnalysisState.ANALYZING, AnalysisState.QUEUED_AI)) { Text("Run AI analysis") }; TextButton(onClick = { vm.delete(id); nav.popBackStack() }) { Text("Delete record") } }
                                Text("This result is not a guarantee. You decide.")
                            }
                        }
                    }
                } ?: Text("Record deleted or unavailable", Modifier.padding(24.dp))
            }
        }
    }
}
