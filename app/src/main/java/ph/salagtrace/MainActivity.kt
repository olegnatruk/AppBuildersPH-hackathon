package ph.salagtrace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import android.provider.Settings
import android.net.Uri
import android.Manifest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ph.salagtrace.domain.AppLanguage
import ph.salagtrace.ui.theme.SalagTheme
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
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !(settings?.darkTheme ?: true)
                    isAppearanceLightNavigationBars = !(settings?.darkTheme ?: true)
                }
            }
            SalagTheme(settings?.language ?: AppLanguage.FILIPINO, settings?.darkTheme ?: true) {
                SalagNavHost(vm, pendingId, { pendingId = null }, { vm.refreshPermissions(this) },
                    { !shouldShowRequestPermissionRationale(Manifest.permission.RECEIVE_SMS) },
                    { !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) },
                    { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                    { startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName)) })
            }
        }
    }
    override fun onResume() { super.onResume(); if (::vm.isInitialized) { vm.refreshPermissions(this); vm.rescanModels() } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); pendingId = intent.getLongExtra("messageId", -1).takeIf { it > 0 } }
}
