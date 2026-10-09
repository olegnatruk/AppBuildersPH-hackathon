package ph.salagtrace.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.data.AppSettings
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*
import ph.salagtrace.ui.*
import ph.salagtrace.ui.components.*
import ph.salagtrace.ui.onboarding.ImportProgress
import ph.salagtrace.ui.theme.PlexMono
import java.io.File

@Composable fun SettingsScreen(vm: AppViewModel, settings: AppSettings, permissions: PermissionState, file: File?, models: List<File>, engine: EngineState, importing: ImportState, selfTest: SelfTestState, askSms: () -> Unit, import: () -> Unit) {
    val context = LocalContext.current
    val privateDirectory = context.filesDir.toPath()
    var licenseText by remember { mutableStateOf<String?>(null) }
    var reset by remember { mutableStateOf(false) }; var remove by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<Int?>(null) }
    LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Text(text(R.string.settings), style = MaterialTheme.typography.headlineLarge) }
        item { BoardCard {
            StatusRow(Icons.Outlined.Sms,text(R.string.monitoring),text(if(settings.monitoringEnabled && permissions.sms) R.string.on else R.string.off)) {
                Switch(settings.monitoringEnabled && permissions.sms,{ if(it && !permissions.sms) askSms() else vm.setMonitoring(it) })
            }
            HorizontalDivider(); SectionTitle(R.string.scope)
            Choice(text(R.string.scope_flagged),settings.aiScope == AiScope.FLAGGED) { vm.setScope(AiScope.FLAGGED) }
            Choice(text(R.string.scope_all),settings.aiScope == AiScope.ALL) { vm.setScope(AiScope.ALL) }
        } }
        item { BoardCard {
            SectionTitle(R.string.language)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(settings.language == AppLanguage.FILIPINO,{ vm.setLanguage(AppLanguage.FILIPINO) },{ Text("Filipino") }); FilterChip(settings.language == AppLanguage.ENGLISH,{ vm.setLanguage(AppLanguage.ENGLISH) },{ Text("English") }) }
            SectionTitle(R.string.appearance)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(settings.darkTheme,{ vm.setDark(true) },{ Text(text(R.string.dark)) }); FilterChip(!settings.darkTheme,{ vm.setDark(false) },{ Text(text(R.string.light)) }) }
        } }
        item { BoardCard {
            SectionTitle(R.string.history)
            listOf(7,30).forEach { days -> Choice(text(R.string.retention,days),settings.retentionDays == days) { vm.setRetention(days) } }
            TextButton(onClick = { reset = true }) { Text(text(R.string.delete_all_data), color = MaterialTheme.colorScheme.error) }
        } }
        item { BoardCard {
            SectionTitle(R.string.model)
            Text(file?.let { modelLabel(it.name) } ?: text(R.string.missing), style = MaterialTheme.typography.titleMedium)
            Text(modelState(file,engine), style = MaterialTheme.typography.bodySmall)
            file?.let { Text("${decimal(it.length()/1_000_000.0)} MB\n${it.absolutePath}", fontFamily = PlexMono, style = MaterialTheme.typography.bodySmall) }
            if(models.size > 1) models.forEach { candidate -> Choice(modelLabel(candidate.name) + " · " + if(candidate.toPath().startsWith(privateDirectory)) "private" else "external", candidate.absolutePath == file?.absolutePath) { vm.selectModel(candidate) } }
            Button(onClick = import, enabled = importing !is ImportState.Copying, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.model_import)) }
            ImportProgress(importing)
            TextButton(onClick = vm::rescanModels) { Text(text(R.string.rescan)) }
            if(file != null) TextButton(onClick = { remove = true }) { Text(text(R.string.remove_model), color = MaterialTheme.colorScheme.error) }
            HorizontalDivider()
            Text(text(R.string.self_test_body), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = vm::runSelfTest, enabled = file != null && selfTest !is SelfTestState.Running && engine !is EngineState.Busy, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.self_test)) }
            when(selfTest) {
                SelfTestState.Running -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(text(R.string.analyzing)); TextButton(onClick = vm::cancelSelfTest) { Text(text(R.string.cancel)) } }
                is SelfTestState.Failed -> Text(text(R.string.self_failed), color = MaterialTheme.colorScheme.error)
                is SelfTestState.Done -> {
                    VerdictBadge(selfTest.verdict.verdict)
                    val c = selfTest.completion; val r = c.result
                    Text("load ${c.loadMs} ms\nprompt ${r.promptTokens} tokens · ${decimal(if(r.prefillMs > 0) r.promptTokens * 1000.0/r.prefillMs else 0.0)} tok/s\ngeneration ${r.genTokens} tokens · ${decimal(if(r.genMs > 0) r.genTokens * 1000.0/r.genMs else 0.0)} tok/s\ntotal ${decimal(c.latencyMs/1000.0)} s · offline", fontFamily = PlexMono, style = MaterialTheme.typography.bodySmall)
                }
                else -> Unit
            }
        } }
        item { BoardCard {
            SectionTitle(R.string.privacy)
            Text(text(R.string.offline))
            TextButton(onClick = { info = R.string.stored_body }) { Text(text(R.string.stored_title)) }
            Text("adb shell dumpsys package ph.salagtrace", fontFamily = PlexMono, style = MaterialTheme.typography.bodySmall)
        } }
        item { BoardCard { SectionTitle(R.string.limits); Text(text(R.string.limits_body), style = MaterialTheme.typography.bodySmall) } }
        item { BoardCard { SectionTitle(R.string.about); Text(text(R.string.about_body), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { licenseText = context.assets.list("licenses").orEmpty().sorted().joinToString("\n\n") { name -> name + "\n\n" + context.assets.open("licenses/$name").bufferedReader().use { it.readText() } } }) { Text(text(R.string.licenses)) }
        } }
    }
    if(reset) ConfirmDialog(R.string.delete_all_data,R.string.reset_body,{ reset = false },{ vm.deleteAll(true); reset = false })
    if(remove) ConfirmDialog(R.string.remove_model,R.string.remove_model_body,{ remove = false },{ vm.removeModel(); remove = false })
    licenseText?.let { body -> AlertDialog(onDismissRequest = { licenseText = null },title = { Text(text(R.string.licenses)) },text = { Text(body,Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),style = MaterialTheme.typography.bodySmall) },confirmButton = { TextButton(onClick = { licenseText = null }) { Text(text(R.string.ok)) } }) }
    info?.let { id -> AlertDialog(onDismissRequest = { info = null },title = { Text(text(R.string.stored_title)) },text = { Text(text(id)) },confirmButton = { TextButton(onClick = { info = null }) { Text(text(R.string.ok)) } }) }
}
@Composable private fun Choice(label: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected,choose); TextButton(onClick = choose, modifier = Modifier.weight(1f)) { Text(label, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium) } }
}
