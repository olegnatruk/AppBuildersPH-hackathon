package ph.salagtrace.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ph.salagtrace.AppContainer
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*

data class PermissionState(val sms: Boolean = false, val notifications: Boolean = false)
sealed interface SelfTestState {
    data object Idle : SelfTestState
    data object Running : SelfTestState
    data class Done(val completion: LlmCompletion, val verdict: FinalVerdict) : SelfTestState
    data class Failed(val reason: String) : SelfTestState
}
class AppViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val messages = c.database.messages().observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val engine = c.engine.state
    val activeModel = c.models.active
    val models = c.models.models
    val importing = c.models.importing
    private val _permissions = MutableStateFlow(PermissionState())
    val permissions = _permissions.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _selfTest = MutableStateFlow<SelfTestState>(SelfTestState.Idle)
    val selfTest = _selfTest.asStateFlow()
    fun message(id: Long) = c.database.messages().observeById(id)
    fun refreshPermissions(context: Context) {
        _permissions.value = PermissionState(context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED,
            if (Build.VERSION.SDK_INT >= 33) context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED && context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
            else context.getSystemService(NotificationManager::class.java).areNotificationsEnabled())
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { _error.value = "Action could not finish. Please try again." }
    }
    fun clearError() { _error.value = null }
    fun consent() = action { c.settings.consent() }
    fun finishOnboarding() = action { c.settings.setMonitoring(_permissions.value.sms); c.settings.finishOnboarding() }
    fun setMonitoring(value: Boolean) = action { if (value && !c.settings.setMonitoring(true)) _error.value = "Consent and SMS permission are needed." else if (!value) c.settings.setMonitoring(false) }
    fun setDark(value: Boolean) = action { c.settings.setDark(value) }
    fun markSmsAsked() = action { c.settings.markSmsAsked() }
    fun markNotificationsAsked() = action { c.settings.markNotificationsAsked() }
    fun setLanguage(value: AppLanguage) = action { c.settings.setLanguage(value) }
    fun setScope(value: AiScope) = action { c.settings.setAiScope(value) }
    fun setRetention(value: Int) = action { c.settings.setRetention(value); c.messages.purge() }
    fun rescanModels() = action { c.models.rescan() }
    fun selectModel(file: java.io.File) = action { c.models.select(file) }
    fun importModel(uri: Uri) = action { c.models.import(uri) }
    fun removeModel() = action { c.models.removeActive() }
    fun sample(onSaved: (Long) -> Unit) = action { val id = c.messages.accept("GCash sample", SpikePrompt.BODY, System.currentTimeMillis(), Source.SAMPLE); onSaved(id) }
    fun submit(sender: String, body: String, onSaved: (Long) -> Unit) = action { val id = c.messages.accept(sender.ifBlank { "Manual input" }, body, System.currentTimeMillis(), Source.MANUAL); onSaved(id) }
    fun rerun(id: Long) = action { c.messages.rerun(id) }
    fun cancel(id: Long) = action {
        c.engine.cancel(id); c.scheduler.cancel(id)
        val m = c.database.messages().byId(id) ?: return@action
        c.database.messages().update(m.copy(state = AnalysisState.AI_FAILED, error = "Analysis canceled. Rule check remains available.", modelName = null))
    }
    fun delete(id: Long) = action { c.engine.cancel(id); c.scheduler.cancel(id); c.database.messages().deleteById(id); c.notifier.cancel(id) }
    fun deleteAll(resetConsent: Boolean = false) = action {
        c.engine.cancel(); c.scheduler.cancelAll(); c.database.messages().deleteAll(); c.notifier.cancelAll()
        if (resetConsent) c.settings.resetConsent()
    }
    fun runSelfTest() = action {
        _selfTest.value = SelfTestState.Running
        try {
            val file = c.models.activeModel() ?: throw ModelLoadException()
            val body = Masker.mask(SpikePrompt.BODY).body
            val rules = Screener.screen(body)
            val prompt = PromptBuilder.build(body, "GCash sample", rules.signals, settings.value?.language ?: AppLanguage.FILIPINO)
            val completion = c.engine.complete(file, prompt.system, prompt.user, -1, grammar = ScamGrammar.requiringEvidence())
            if (completion.result.stop !in setOf("eos", "max")) throw ModelLoadException()
            val result = (ResponseParser.parse(completion.result.text) as? ParseOutcome.Success)?.analysis ?: throw ModelLoadException()
            _selfTest.value = SelfTestState.Done(completion, VerdictFusion.fuse(result, rules, EvidenceValidator.validate(result, body)))
        } catch (_: Exception) { _selfTest.value = SelfTestState.Failed("Self-test could not finish. Check the model, free memory, and retry.") }
    }
    fun cancelSelfTest() { c.engine.cancel(-1) }
}
