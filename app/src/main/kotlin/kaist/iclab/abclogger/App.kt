package kaist.iclab.abclogger

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import github.agustarc.koap.Koap
import io.fabric.sdk.android.Fabric


class App : Application(){
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        WorkManager.initialize(this, Configuration.Builder().build())

        Fabric.with(Fabric.Builder(this)
            .kits(Crashlytics())
            .debuggable(true)
            .build())

        ObjBox.bind(this)

        Koap.bind(this, SharedPrefs)

    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate()")

    }

    companion object {
        val TAG = App::class.java.canonicalName
    }
}
