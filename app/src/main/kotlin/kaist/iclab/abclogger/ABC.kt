package kaist.iclab.abclogger

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import github.agustarc.koap.Koap
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.base.BaseService
import kaist.iclab.abclogger.collector.hasStarted
import kaist.iclab.abclogger.collector.start
import kaist.iclab.abclogger.collector.stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class ABC(vararg collector: BaseCollector) {
    val collectors = arrayOf(*collector)
    val maps = collectors.associateBy { it::class.java }

    suspend fun startAll(
            onComplete: ((collector: BaseCollector, throwable: Throwable?) -> Unit)? = null
    ) = collectors.forEach { if (it.hasStarted()) it.start(onComplete) }

    suspend fun stopAll(
            onComplete: ((collector: BaseCollector, throwable: Throwable?) -> Unit)? = null
    ) = collectors.forEach { if (it.hasStarted()) it.stop(onComplete) }

    fun isAllAvailable() = collectors.filter { it.hasStarted() }.all { it.checkAvailability() }

    fun hasAnyStarted() = collectors.any { it.hasStarted() }

    fun getAllRequiredPermissions() = collectors.map { it.requiredPermissions }.flatten()

    inline fun <reified T : BaseCollector> get() = maps[T::class.java]

    companion object {
        private fun startService(context: Context) {
            if(!context.checkServiceRunning<ABCLoggerService>()) context.startForegroundService<ABCLoggerService>()
        }

        private fun stopService(context: Context) {
            if(context.checkServiceRunning<ABCLoggerService>()) context.stopService<ABCLoggerService>()
        }

        fun doAfterSignIn(context: Context) {
            FirebaseCrashlytics.getInstance().setUserId(FirebaseAuth.getInstance().currentUser?.email ?: "")
            startService(context)
        }

        suspend fun signOut(context: Context) {
            ObjBox.flush(context)
            GeneralPrefs.clear()
            CollectorPrefs.clear()
            FirebaseAuth.getInstance().signOut()
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().toCoroutine()
            stopService(context)
        }
    }

    class ABCLoggerService : BaseService() {
        private val abcLogger: ABC by inject()

        override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
            GlobalScope.launch(Dispatchers.IO) {
                abcLogger.startAll()
            }
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val ntf = Notifications.build(
                    context = this,
                    channelId = Notifications.CHANNEL_ID_FOREGROUND,
                    title = getString(R.string.ntf_title_service_running)
            )

            startForeground(Notifications.ID_FOREGROUND, ntf)

            return START_STICKY
        }

        override fun onDestroy() {
            super.onDestroy()

            GlobalScope.launch(Dispatchers.IO) {
                abcLogger.stopAll()
            }
        }
    }
}
