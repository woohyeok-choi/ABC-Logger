package kaist.iclab.abclogger

import android.app.Application
import android.util.Log
import github.agustarc.koap.Koap
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application(){
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")

        ObjBox.bind(this)
        Notifications.bind(this)
        Koap.bind(this, CollectorPrefs, GeneralPrefs)
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(listOf(collectorModules, viewModelModules))
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
