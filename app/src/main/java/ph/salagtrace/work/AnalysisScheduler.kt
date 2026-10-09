package ph.salagtrace.work

import android.content.Context
import androidx.work.*
import ph.salagtrace.data.MessageDao
import ph.salagtrace.domain.AnalysisState

class AnalysisScheduler(context: Context) {
    private val manager = WorkManager.getInstance(context)
    fun enqueue(id: Long) {
        val request = OneTimeWorkRequestBuilder<AnalysisWorker>()
            .setInputData(workDataOf("messageId" to id))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag("analysis").build()
        manager.enqueueUniqueWork("analysis-$id", ExistingWorkPolicy.KEEP, request)
    }
    fun cancel(id: Long) { manager.cancelUniqueWork("analysis-$id") }
    fun cancelAll() { manager.cancelAllWorkByTag("analysis") }
    suspend fun recover(dao: MessageDao) {
        val staleBefore = System.currentTimeMillis() - 300_000
        dao.pendingAi().forEach { row ->
            val work = manager.getWorkInfosForUniqueWork("analysis-${row.id}").get()
            val hasLiveWork = work.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }
            if (row.state == AnalysisState.QUEUED_AI || !hasLiveWork || (row.analysisStartedAt ?: 0) < staleBefore) {
                if (row.state == AnalysisState.ANALYZING && !hasLiveWork) dao.setState(row.id, AnalysisState.QUEUED_AI)
                enqueue(row.id)
            }
        }
    }
}
