package ph.salagtrace

import android.app.Application
import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.*
import ph.salagtrace.data.*

class SalagApp : Application() {
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch { container.messages.purge() }
    }
}

class AppContainer(context: Context) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, "salag.db").build()
    val settings = SettingsRepository(context)
    val messages = MessageRepository(database.messages(), settings)
}
