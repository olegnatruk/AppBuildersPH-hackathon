package ph.salagtrace.ui.messages

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ph.salagtrace.R
import ph.salagtrace.data.ScannedMessageEntity
import ph.salagtrace.domain.*
import ph.salagtrace.ui.AppViewModel
import ph.salagtrace.ui.components.*

@Composable fun MessagesScreen(vm: AppViewModel, rows: List<ScannedMessageEntity>, openDetail: (Long) -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<Long?>(null) }
    var all by remember { mutableStateOf(false) }
    val labels = listOf(R.string.all,R.string.scam,R.string.suspicious,R.string.uncertain,R.string.safe,R.string.pending)
    val filtered = rows.filter { when(filter) {
        1 -> it.verdict == Verdict.LIKELY_SCAM; 2 -> it.verdict == Verdict.SUSPICIOUS
        3 -> it.verdict == Verdict.UNCERTAIN; 4 -> it.verdict == Verdict.LIKELY_SAFE
        5 -> it.state in setOf(AnalysisState.QUEUED_AI,AnalysisState.ANALYZING); else -> true
    } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(text(R.string.messages), style = MaterialTheme.typography.headlineMedium)
            TextButton(onClick = { all = true }, enabled = rows.isNotEmpty()) { Text(text(R.string.delete_all), style = MaterialTheme.typography.labelSmall) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { labels.forEachIndexed { i,label -> FilterChip(filter == i, { filter = i }, { Text(text(label)) }) } }
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if(filtered.isEmpty()) item { BoardCard { Text(text(R.string.empty_messages)) } }
            items(filtered, key = { it.id }) { m -> MessageRow(m, { openDetail(m.id) }, { deleting = m.id }) }
        }
    }
    deleting?.let { id -> ConfirmDialog(R.string.delete_title,R.string.delete_body,{ deleting = null },{ vm.delete(id); deleting = null }) }
    if(all) ConfirmDialog(R.string.delete_all,R.string.delete_all_body,{ all = false },{ vm.deleteAll(); all = false })
}
