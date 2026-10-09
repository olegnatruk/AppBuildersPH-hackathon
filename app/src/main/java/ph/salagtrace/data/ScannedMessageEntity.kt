package ph.salagtrace.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ph.salagtrace.domain.*

val StorageJson = Json { ignoreUnknownKeys = true }
inline fun <reified T> decodeStored(value: String, fallback: T): T = runCatching { StorageJson.decodeFromString<T>(value) }.getOrDefault(fallback)

@Entity(tableName = "scanned_messages", indices = [Index(value = ["dedupKey"], unique = true), Index(value = ["receivedAt"])])
data class ScannedMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dedupKey: String, val source: Source, val sender: String, val maskedBody: String,
    val maskSpansJson: String, val receivedAt: Long, val ruleScore: Int,
    val ruleSignalsJson: String, val urlsJson: String, val state: AnalysisState,
    val verdict: Verdict, val confidence: Confidence = Confidence.LOW, val category: Category = Category.NONE,
    val tacticsJson: String = "[]", val evidenceJson: String = "[]",
    val explanation: String? = null, val explanationLang: String? = null,
    val disagreement: Boolean = false, val droppedEvidence: Int = 0, val notesJson: String = "[]",
    val modelName: String? = null, val latencyMs: Long? = null, val loadMs: Long? = null,
    val promptTokens: Int? = null, val genTokens: Int? = null,
    val prefillMs: Double? = null, val genMs: Double? = null,
    val error: String? = null, val analyzedAt: Long? = null, val analysisStartedAt: Long? = null,
) {
    fun rules() = RuleResult(ruleScore, decodeStored(ruleSignalsJson, emptyList<RuleSignal>()), decodeStored(urlsJson, emptyList<UrlInfo>()))
    fun evidence() = decodeStored(evidenceJson, emptyList<VerifiedEvidence>())
    fun masks() = decodeStored(maskSpansJson, emptyList<MaskSpan>())
    fun tactics() = decodeStored(tacticsJson, emptyList<Tactic>())
    fun notes() = decodeStored(notesJson, emptyList<String>())
    fun isAiResult() = state == AnalysisState.DONE && modelName != null
}
