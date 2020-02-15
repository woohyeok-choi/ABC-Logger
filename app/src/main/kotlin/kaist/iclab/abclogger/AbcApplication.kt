package kaist.iclab.abclogger

import android.app.Application
import kaist.iclab.abclogger.commons.AppLog
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class AbcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.d(TAG, "onCreate()")
        startKoin {
            androidLogger()
            androidContext(this@AbcApplication)
            modules(listOf(collectorModules, viewModelModules))
        }
        GlobalScope.launch {
            AbcCollector.bind(this@AbcApplication)

            if (BuildConfig.IS_TEST_MODE) {
                Debug.generateEntities(500000)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        AppLog.d(TAG, "onTerminate()")
    }

    companion object {
        val TAG = AbcApplication::class.java.canonicalName
    }
}
