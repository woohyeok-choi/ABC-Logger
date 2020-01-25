package kaist.iclab.abclogger

import android.app.Application
import android.util.Log
import github.agustarc.koap.Koap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application(){
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(collectorModules, viewModelModules))
        }
        Notifications.bind(this)

        GlobalScope.launch (Dispatchers.IO) {
            Koap.bind(this@App, CollectorPrefs, GeneralPrefs)
            ObjBox.bind(this@App)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate()")

    }

    companion object {
        val TAG = App::class.java.canonicalName
    }
}
