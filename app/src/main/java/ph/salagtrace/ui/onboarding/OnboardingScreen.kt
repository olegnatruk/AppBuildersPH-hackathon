package ph.salagtrace.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.data.AppSettings
import ph.salagtrace.llm.*
import ph.salagtrace.ui.*
import ph.salagtrace.ui.components.*
import java.io.File

@Composable fun OnboardingScreen(vm: AppViewModel, settings: AppSettings, permissions: PermissionState, file: File?, importing: ImportState, askSms: () -> Unit, askAlerts: () -> Unit, import: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(if(settings.consentAt > 0) 2 else 0) }
    var consent by rememberSaveable { mutableStateOf(false) }
    var stored by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SALAG TRACE", style = MaterialTheme.typography.labelLarge)
            Text("0${step + 1} / 03", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
        when(step) {
            0 -> {
                Spacer(Modifier.height(32.dp)); Image(painterResource(R.drawable.ic_salag), null, Modifier.size(80.dp))
                Text(text(R.string.tagline), style = MaterialTheme.typography.displayLarge)
                Text(text(R.string.welcome), style = MaterialTheme.typography.headlineMedium)
                Text(text(R.string.welcome_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Tag(text(R.string.offline))
                Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.next)) }
            }
            1 -> {
                Text(text(R.string.privacy_title), style = MaterialTheme.typography.headlineLarge)
                BoardCard { Text(text(R.string.privacy_promise), style = MaterialTheme.typography.bodyLarge) }
                TextButton(onClick = { stored = true }) { Text(text(R.string.stored_title)) }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(consent, { consent = it }); Text(text(R.string.consent), modifier = Modifier.weight(1f)) }
                Button(onClick = { vm.consent(); step = 2 }, enabled = consent, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.next)) }
                TextButton(onClick = { step = 0 }) { Text(text(R.string.back)) }
            }
            else -> {
                Text(text(R.string.permissions_title), style = MaterialTheme.typography.headlineLarge)
                BoardCard {
                    StatusRow(Icons.Outlined.Sms, text(R.string.sms_permission), text(if(permissions.sms) R.string.granted else R.string.denied)) { if(!permissions.sms) TextButton(onClick = askSms) { Text(text(R.string.allow)) } }
                    Text(text(R.string.sms_permission_body), style = MaterialTheme.typography.bodySmall)
                }
                BoardCard {
                    StatusRow(Icons.Outlined.Notifications, text(R.string.notifications), text(if(permissions.notifications) R.string.granted else R.string.denied)) { if(!permissions.notifications) TextButton(onClick = askAlerts) { Text(text(R.string.allow)) } }
                    Text(text(R.string.notifications_body), style = MaterialTheme.typography.bodySmall)
                }
                BoardCard {
                    Text(text(R.string.model), style = MaterialTheme.typography.titleMedium)
                    Text(file?.let { modelLabel(it.name) } ?: text(R.string.missing))
                    OutlinedButton(onClick = import, enabled = importing !is ImportState.Copying) { Text(text(R.string.model_import)) }
                    ImportProgress(importing)
                }
                Button(onClick = { vm.finishOnboarding() }, enabled = settings.consentAt > 0, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.finish)) }
                Tag(text(R.string.offline))
            }
        }
    }
    if(stored) AlertDialog(onDismissRequest = { stored = false }, title = { Text(text(R.string.stored_title)) }, text = { Text(text(R.string.stored_body)) }, confirmButton = { TextButton(onClick = { stored = false }) { Text(text(R.string.ok)) } })
}
@Composable fun ImportProgress(state: ImportState) {
    when(state) {
        is ImportState.Copying -> {
            if(state.total > 0) LinearProgressIndicator(progress = { (state.bytes.toDouble() / state.total).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(text(R.string.importing, state.bytes / 1_000_000), style = MaterialTheme.typography.bodySmall)
        }
        is ImportState.Done -> Text(text(R.string.import_done), style = MaterialTheme.typography.bodySmall)
        is ImportState.Error -> Text(text(R.string.import_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        else -> Unit
    }
}
