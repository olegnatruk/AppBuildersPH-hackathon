package ph.salagtrace.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import ph.salagtrace.domain.*
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("salag_settings")
data class AppSettings(
    val consentAt: Long = 0, val onboardingDone: Boolean = false, val monitoringEnabled: Boolean = false,
    val aiScope: AiScope = AiScope.FLAGGED, val language: AppLanguage = AppLanguage.FILIPINO,
    val retentionDays: Int = 7, val activeModel: String = "",
)
class SettingsRepository(private val context: Context) {
    private object Keys {
        val consent = longPreferencesKey("consent_at")
        val onboarding = booleanPreferencesKey("onboarding_done")
        val monitoring = booleanPreferencesKey("monitoring_enabled")
        val scope = stringPreferencesKey("ai_scope")
        val language = stringPreferencesKey("language")
        val retention = intPreferencesKey("retention_days")
        val model = stringPreferencesKey("active_model")
    }
    val settings: Flow<AppSettings> = context.settingsStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }.map { p ->
        AppSettings(p[Keys.consent] ?: 0, p[Keys.onboarding] ?: false, p[Keys.monitoring] ?: false,
            runCatching { AiScope.valueOf(p[Keys.scope] ?: "FLAGGED") }.getOrDefault(AiScope.FLAGGED),
            runCatching { AppLanguage.valueOf(p[Keys.language] ?: "FILIPINO") }.getOrDefault(AppLanguage.FILIPINO),
            (p[Keys.retention] ?: 7).let { if (it == 30) 30 else 7 }, p[Keys.model].orEmpty())
    }
    suspend fun consent() = context.settingsStore.edit { it[Keys.consent] = System.currentTimeMillis() }
    suspend fun finishOnboarding() = context.settingsStore.edit { it[Keys.onboarding] = true }
    suspend fun setLanguage(value: AppLanguage) = context.settingsStore.edit { it[Keys.language] = value.name }
    suspend fun setAiScope(value: AiScope) = context.settingsStore.edit { it[Keys.scope] = value.name }
    suspend fun setRetention(days: Int) = context.settingsStore.edit { it[Keys.retention] = if (days == 30) 30 else 7 }
    suspend fun setActiveModel(path: String) = context.settingsStore.edit { it[Keys.model] = path }
    suspend fun setMonitoring(enabled: Boolean): Boolean {
        val allowed = enabled && settings.first().consentAt > 0 && context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        if (!allowed) setReceiver(false)
        context.settingsStore.edit { it[Keys.monitoring] = allowed }
        if (allowed) setReceiver(true)
        return allowed
    }
    fun setReceiver(enabled: Boolean) = context.packageManager.setComponentEnabledSetting(
        ComponentName(context, "ph.salagtrace.sms.SmsReceiver"),
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP)
    suspend fun resetConsent() {
        setReceiver(false)
        context.settingsStore.edit { it[Keys.consent] = 0; it[Keys.onboarding] = false; it[Keys.monitoring] = false }
    }
}
