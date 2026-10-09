package ph.salagtrace.ui.check

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.ui.AppViewModel
import ph.salagtrace.ui.components.*

@Composable fun CheckScreen(vm: AppViewModel, openDetail: (Long) -> Unit) {
    // Pasted raw content stays only in this composition, never in saved instance state.
    var sender by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }; var saving by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(text(R.string.check_message), style = MaterialTheme.typography.headlineLarge)
        Text(text(R.string.manual_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Tag("MANUAL")
        OutlinedTextField(sender, { sender = it.take(100) }, label = { Text(text(R.string.sender_optional)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(body, { body = it.take(10_000) }, label = { Text(text(R.string.message_body)) }, minLines = 7, modifier = Modifier.fillMaxWidth())
        Button(onClick = { saving = true; vm.submit(sender, body) { id -> body = ""; sender = ""; saving = false; openDetail(id) } }, enabled = body.isNotBlank() && !saving, modifier = Modifier.fillMaxWidth()) { Text(text(R.string.analyze)) }
        Tag(text(R.string.offline))
    }
}
