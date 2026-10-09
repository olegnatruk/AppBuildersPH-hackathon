package ph.salagtrace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ph.salagtrace.ui.AppViewModel
import ph.salagtrace.ui.nav.SalagNavHost

class MainActivity : ComponentActivity() {
    private lateinit var vm: AppViewModel
    private var pendingId by mutableStateOf<Long?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel((application as SalagApp).container) as T
        })[AppViewModel::class.java]
        vm.refreshPermissions(this)
        pendingId = intent.getLongExtra("messageId", -1).takeIf { it > 0 }
        setContent { MaterialTheme { SalagNavHost(vm, pendingId, { pendingId = null }, { vm.refreshPermissions(this) }) } }
    }
    override fun onResume() { super.onResume(); if (::vm.isInitialized) { vm.refreshPermissions(this); vm.rescanModels() } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); pendingId = intent.getLongExtra("messageId", -1).takeIf { it > 0 } }
}
