package ph.salagtrace

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import ph.salagtrace.data.*
import ph.salagtrace.llm.*

class SalagApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch { container.messages.purge(); container.models.rescan() }
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
    val messages = MessageRepository(database.messages(), settings)
}
