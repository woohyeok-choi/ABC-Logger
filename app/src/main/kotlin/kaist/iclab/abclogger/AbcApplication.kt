package kaist.iclab.abclogger

import android.app.Application
import github.agustarc.koap.Koap
import kaist.iclab.abclogger.commons.isServiceRunning
import kaist.iclab.abclogger.core.CollectorRepository
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.core.NotificationRepository
import kaist.iclab.abclogger.core.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AbcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(javaClass, "onCreate()")

        NotificationRepository.bind(this)

        startKoin {
            androidLogger(Level.NONE)
            androidContext(this@AbcApplication)
            modules(listOf(collectorModules, repositoryModules, viewModelModules))
        }

        Koap.bind(this, Preference)

        GlobalScope.launch {
            if (BuildConfig.GENERATE_DUMMY_ENTITY) {
                /**
                 * TODO: GENERATE DUMMY ENTITIES
                 */
            }
        }

        CollectorRepository.restart(applicationContext, System.currentTimeMillis())
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(javaClass, "onTerminate()")
    }
}
