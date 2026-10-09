package ph.salagtrace

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import ph.salagtrace.notify.AlertNotifier
import ph.salagtrace.data.*
import ph.salagtrace.llm.*
import ph.salagtrace.work.*

class SalagApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch { container.messages.purge(); container.models.rescan(); container.scheduler.recover(container.database.messages()) }
    }
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) container.scope.launch { container.engine.unloadIfIdle() }
    }
}

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "salag.db").build()
    val settings = SettingsRepository(context)
    val engine = LlmEngine(scope)
    val models = ModelManager(context, settings) { engine.unload() }
    val scheduler = AnalysisScheduler(context)
    val notifier = AlertNotifier(context)
    val messages = MessageRepository(database.messages(), settings, scheduler::enqueue) { row ->
        scope.launch { notifier.screened(row, settings.settings.first().language) }
    }
    var onAnalyzed: suspend (Long) -> Unit = { id ->
        database.messages().byId(id)?.let { notifier.analyzed(it, settings.settings.first().language) }
    }
}
