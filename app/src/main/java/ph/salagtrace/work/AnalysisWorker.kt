package ph.salagtrace.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import ph.salagtrace.BuildConfig
import ph.salagtrace.SalagApp
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*

class AnalysisWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val container get() = (applicationContext as SalagApp).container
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("analysis", "On-device analysis", NotificationManager.IMPORTANCE_LOW))
        val notification = Notification.Builder(applicationContext, "analysis")
            .setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("SALAG TRACE")
            .setContentText("Analyzing a message on this device").setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build()
        return ForegroundInfo(1001, notification)
    }
    override suspend fun doWork(): Result {
        val id = inputData.getLong("messageId", -1)
        val c = container
        val row = c.database.messages().byId(id) ?: return Result.success()
        if (row.state !in setOf(AnalysisState.QUEUED_AI, AnalysisState.ANALYZING)) return Result.success()
        try {
            val model = c.models.activeModel()
            if (model == null) { fail(row, AnalysisState.AI_UNAVAILABLE, "Install a GGUF model to run on-device AI. Rule check is available."); return Result.success() }
            val preferences = c.settings.settings.first()
            val prompt = PromptBuilder.build(row.maskedBody, row.sender, row.rules().signals, preferences.language)
            val timeout = if (BuildConfig.DEBUG) inputData.getLong("timeoutMs", 90_000).coerceIn(1, 90_000) else 90_000L
            val tokens = if (BuildConfig.DEBUG) inputData.getInt("maxTokens", 320).coerceIn(1, 320) else 320
            var last: LlmCompletion? = null; var parsed: AiAnalysis? = null; var totalMs = 0L; var totalLoadMs = 0L
            var retried = false
            for (attempt in 0..1) {
                val completion = c.engine.complete(model, prompt.system, prompt.user, id, temperature = if (attempt == 0) 0.2f else 0.0f, grammar = if (row.rules().needsAi(AiScope.FLAGGED)) ScamGrammar.requiringEvidence() else ScamGrammar.GBNF, timeoutMs = timeout, maxTokens = tokens) {
                    c.database.messages().setState(id, AnalysisState.ANALYZING, System.currentTimeMillis())
                }
                last = completion; totalMs += completion.latencyMs; totalLoadMs += completion.loadMs
                when (completion.result.stop) {
                    "timeout" -> { fail(row, AnalysisState.AI_TIMEOUT, "Local AI reached its time limit. You can retry."); return Result.success() }
                    "cancel" -> { fail(row, AnalysisState.AI_FAILED, "Analysis canceled. Rule check remains available."); return Result.success() }
                    "error" -> { fail(row, AnalysisState.AI_FAILED, "Local AI could not run. Check the model and available memory."); return Result.success() }
                }
                val outcome = ResponseParser.parse(completion.result.text)
                if (outcome is ParseOutcome.Success) { parsed = outcome.analysis; break }
                retried = true
            }
            val ai = parsed
            val completion = last
            if (ai == null || completion == null) { fail(row, AnalysisState.AI_FAILED, "AI returned invalid structured output after one retry."); return Result.success() }
            val validated = EvidenceValidator.validate(ai, row.maskedBody)
            val final = VerdictFusion.fuse(ai, row.rules(), validated)
            val notes = final.notes + (if (prompt.truncated) listOf("TRUNCATED_INPUT") else emptyList()) + (if (retried) listOf("AI_RETRIED") else emptyList())
            // @Update never resurrects a record deleted while native inference was running.
            c.database.messages().update(row.copy(state = AnalysisState.DONE, verdict = final.verdict,
                confidence = final.confidence, category = final.category, tacticsJson = StorageJson.encodeToString(final.tactics),
                evidenceJson = StorageJson.encodeToString(final.evidence), explanation = ai.explanation, explanationLang = preferences.language.name,
                disagreement = final.disagreement, droppedEvidence = validated.dropped, notesJson = StorageJson.encodeToString(notes),
                modelName = completion.modelName, latencyMs = totalMs, loadMs = totalLoadMs,
                promptTokens = completion.result.promptTokens, genTokens = completion.result.genTokens,
                prefillMs = completion.result.prefillMs, genMs = completion.result.genMs, analyzedAt = System.currentTimeMillis(), error = null))
            c.onAnalyzed(id)
            return Result.success()
        } catch (e: CancellationException) {
            withContext(NonCancellable) { fail(row, AnalysisState.AI_FAILED, "Analysis canceled. Rule check remains available.") }
            throw e
        } catch (_: ModelLoadException) {
            fail(row, AnalysisState.AI_FAILED, "Model could not load. Check the GGUF file and available memory.")
        } catch (_: Exception) {
            fail(row, AnalysisState.AI_FAILED, "Local AI could not run. Rule check remains available.")
        }
        return Result.success()
    }
    private suspend fun fail(row: ScannedMessageEntity, state: AnalysisState, error: String) {
        container.database.messages().update(row.copy(state = state, error = error, modelName = null, confidence = Confidence.LOW,
            verdict = Screener.ruleVerdict(row.rules()), evidenceJson = "[]", explanation = null, analyzedAt = System.currentTimeMillis()))
        container.onAnalyzed(row.id)
    }
}
