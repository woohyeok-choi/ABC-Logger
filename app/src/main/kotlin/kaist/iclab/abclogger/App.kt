package kaist.iclab.abclogger

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kaist.iclab.abclogger.data.entities.MyObjectBox
import com.crashlytics.android.Crashlytics
import github.agustarc.koap.Koap
import io.fabric.sdk.android.Fabric
import kaist.iclab.abclogger.data.Prefs

val prefs: Prefs by lazy {
    Log.d("class App", "prefs Lazy")
    App.sharedPreference!!
}

class App : Application(){
    companion object {
        private val TAG = App::class.java.canonicalName
        lateinit var boxStore: BoxStore
        var sharedPreference: Prefs? = null

        inline fun <reified T> boxFor() = boxStore.boxFor<T>()
    }

    class AppLifecycleOwner private constructor() : LifecycleOwner {
        companion object {
            private var instance = AppLifecycleOwner()

            fun getInstance() : AppLifecycleOwner {
                return instance
            }
        }

        private val registry = LifecycleRegistry(this)

        override fun getLifecycle(): Lifecycle {
            return registry
        }

        fun onCreate () {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onDestroy () {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        WorkManager.initialize(this, Configuration.Builder().build())
        FirebaseFirestore.getInstance().firestoreSettings = FirebaseFirestoreSettings.Builder().setTimestampsInSnapshotsEnabled(true).build()

        Fabric.with(Fabric.Builder(this)
            .kits(Crashlytics())
            .debuggable(true)
            .build())
        AppLifecycleOwner.getInstance().onCreate()

        ObjBox.bind(this)
        Koap.bind(this, SharedPrefs)

        sharedPreference = Prefs(applicationContext)
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "onTerminate()")

        AppLifecycleOwner.getInstance().onDestroy()
    }
}
