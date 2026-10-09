package ph.salagtrace.data

import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import ph.salagtrace.domain.*
import ph.salagtrace.sms.SmsAssembler

class MessageRepository(
    val dao: MessageDao, private val settings: SettingsRepository,
    private val schedule: (Long) -> Unit = {},
    private val onScreened: (ScannedMessageEntity) -> Unit = {},
) {
    suspend fun accept(sender: String, body: String, timestamp: Long, source: Source): Long {
        val preferences = settings.settings.first()
        val masked = Masker.mask(body)
        val rules = Screener.screen(masked.body, sender)
        val ai = rules.needsAi(preferences.aiScope)
        val row = ScannedMessageEntity(
            dedupKey = SmsAssembler.dedupKey("${source.name}|$sender", body, timestamp), source = source,
            sender = sender.ifBlank { "Unknown" }, maskedBody = masked.body,
            maskSpansJson = StorageJson.encodeToString(masked.spans), receivedAt = timestamp,
            ruleScore = rules.score, ruleSignalsJson = StorageJson.encodeToString(rules.signals), urlsJson = StorageJson.encodeToString(rules.urls),
            state = if (ai) AnalysisState.QUEUED_AI else AnalysisState.SCREENED, verdict = Screener.ruleVerdict(rules),
            category = ruleCategory(rules),

            tacticsJson = StorageJson.encodeToString(rules.signals.filterNot { it.id.startsWith("benign") || it.id.startsWith("brand") }.map { it.tactic }.distinct()),
        )
        val id = dao.insertIgnore(row)
        if (id == -1L) return dao.idForKey(row.dedupKey) ?: -1
        purge()
        onScreened(row.copy(id = id))
        if (ai) schedule(id)
        return id
    }
    private fun ruleCategory(rules: RuleResult): Category = when {
        rules.signals.any { it.tactic == Tactic.CREDENTIAL_REQUEST } -> Category.OTP_CREDENTIAL_THEFT
        rules.urls.isNotEmpty() -> Category.PHISHING_LINK
        rules.signals.any { it.id.startsWith("family") } -> Category.FAMILY_IMPERSONATION
        rules.score >= 25 -> Category.OTHER_SCAM
        else -> Category.NONE
    }
    suspend fun purge() { dao.deleteOlderThan(System.currentTimeMillis() - settings.settings.first().retentionDays * 86_400_000L) }
    suspend fun rerun(id: Long) {
        val m = dao.byId(id) ?: return
        dao.update(m.copy(state = AnalysisState.QUEUED_AI, verdict = Screener.ruleVerdict(m.rules()), confidence = Confidence.LOW,
            category = ruleCategory(m.rules()), tacticsJson = StorageJson.encodeToString(m.rules().signals.filterNot { it.id.startsWith("benign") || it.id.startsWith("brand") }.map { it.tactic }.distinct()),
            explanationLang = null, evidenceJson = "[]", explanation = null, modelName = null, error = null, disagreement = false,
            notesJson = "[]", droppedEvidence = 0, analyzedAt = null, analysisStartedAt = null,
            latencyMs = null, loadMs = null, promptTokens = null, genTokens = null, prefillMs = null, genMs = null))
        schedule(id)
    }
}
