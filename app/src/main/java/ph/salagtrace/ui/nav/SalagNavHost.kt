package ph.salagtrace.ui.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import ph.salagtrace.R
import ph.salagtrace.ui.*
import ph.salagtrace.ui.components.text
import ph.salagtrace.ui.home.HomeScreen
import ph.salagtrace.ui.messages.MessagesScreen
import ph.salagtrace.ui.detail.DetailScreen
import ph.salagtrace.ui.settings.SettingsScreen
import ph.salagtrace.ui.onboarding.OnboardingScreen
import ph.salagtrace.ui.check.CheckScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SalagNavHost(vm: AppViewModel, pendingId: Long?, onHandled: () -> Unit, onPermissions: () -> Unit, smsSettingsNeeded: () -> Boolean, alertsSettingsNeeded: () -> Boolean, openAppSettings: () -> Unit, openAlertSettings: () -> Unit) {
    val nav = rememberNavController()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val permissions by vm.permissions.collectAsStateWithLifecycle()
    val file by vm.activeModel.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()
    val selfTest by vm.selfTest.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val smsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onPermissions() }
    val alertsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onPermissions() }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importModel) }
    val prefs = settings ?: return
    val askSms: () -> Unit = { if(prefs.smsAsked && smsSettingsNeeded()) openAppSettings() else { vm.markSmsAsked(); smsLauncher.launch(Manifest.permission.RECEIVE_SMS) } }
    val askAlerts: () -> Unit = { if(Build.VERSION.SDK_INT < 33 || (prefs.notificationsAsked && alertsSettingsNeeded())) openAlertSettings() else { vm.markNotificationsAsked(); alertsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } }
    val import: () -> Unit = { importer.launch(arrayOf("*/*")) }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val tabs = listOf(Triple("home",R.string.home,Icons.Outlined.Home),Triple("messages",R.string.messages,Icons.Outlined.Sms),Triple("settings",R.string.settings,Icons.Outlined.Tune))
    LaunchedEffect(pendingId,prefs.onboardingDone) { if(pendingId != null && prefs.onboardingDone) { nav.navigate("detail/$pendingId"); onHandled() } }
    Scaffold(
        topBar = { if(prefs.onboardingDone) TopAppBar(title = { Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Image(painterResource(R.drawable.ic_salag),null,Modifier.size(28.dp)); Text("SALAG TRACE",style = MaterialTheme.typography.titleMedium) } }, navigationIcon = { if(route !in tabs.map { it.first }) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,text(R.string.back)) } }) },
        bottomBar = { if(prefs.onboardingDone && route in tabs.map { it.first }) NavigationBar { tabs.forEach { (target,label,icon) -> NavigationBarItem(route == target,{ nav.navigate(target) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },{ Icon(icon,null) },label = { Text(text(label)) }) } } }
    ) { padding ->
        if(!prefs.onboardingDone) Box(Modifier.padding(padding)) { OnboardingScreen(vm,prefs,permissions,file,importing,askSms,askAlerts,import) }
        else NavHost(nav,"home",modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(vm,prefs,permissions,messages,file,engine,askSms,askAlerts,{ nav.navigate("settings") },{ nav.navigate("check") },{ nav.navigate("detail/$it") }) }
            composable("messages") { MessagesScreen(vm,messages) { nav.navigate("detail/$it") } }
            composable("settings") { SettingsScreen(vm,prefs,permissions,file,models,engine,importing,selfTest,askSms,import) }
            composable("check") { CheckScreen(vm) { nav.navigate("detail/$it") { popUpTo("check") { inclusive = true } } } }
            composable("detail/{id}") { back ->
                val id = back.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                val row by remember(id) { vm.message(id) }.collectAsStateWithLifecycle(null)
                DetailScreen(vm,row,prefs.language,{ nav.popBackStack() },{ nav.navigate("settings") })
            }
        }
    }
    if(error != null) AlertDialog(onDismissRequest = vm::clearError, text = { Text(text(if(error!!.startsWith("Consent")) R.string.permission_needed else R.string.action_failed)) }, confirmButton = { TextButton(onClick = vm::clearError) { Text(text(R.string.ok)) } })
}
