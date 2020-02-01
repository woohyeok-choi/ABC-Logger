package kaist.iclab.abclogger

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import github.agustarc.koap.Koap
import github.agustarc.koap.gson.GsonSerializer
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.start
import kaist.iclab.abclogger.collector.stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class ABC(vararg collector: BaseCollector) {
    val collectors = arrayOf(*collector)
    val maps = collectors.associateBy { it::class.java }

    suspend fun startAll(
            onComplete: ((collector: BaseCollector, throwable: Throwable?) -> Unit)? = null
    ) = collectors.forEach { if (it.getStatus()?.hasStarted == true) it.start(onComplete) }

    suspend fun stopAll(
            onComplete: ((collector: BaseCollector, throwable: Throwable?) -> Unit)? = null
    ) = collectors.forEach { if (it.getStatus()?.hasStarted == true) it.stop(onComplete) }

    fun getAllRequiredPermissions() = collectors.map { it.requiredPermissions }.flatten()

    inline fun <reified T : BaseCollector> get() = maps[T::class.java]

    companion object {
        fun startService(context: Context) {
            if(!checkServiceRunning<ABCLoggerService>(context) && FirebaseAuth.getInstance().currentUser != null)
                ContextCompat.startForegroundService(context, Intent(context, ABCLoggerService::class.java))
        }

        fun stopService(context: Context) {
            if(checkServiceRunning<ABCLoggerService>(context))
                context.stopService(Intent(context, ABCLoggerService::class.java))
        }

        suspend fun bind(context: Context) {
            Notifications.bind(context)
            Koap.serializer = GsonSerializer(Gson())
            Koap.bind(context, Prefs, Prefs)
            ObjBox.bind(context)
        }
    }

    class ABCLoggerService : Service() {
        private val abcLogger: ABC by inject()

        override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
            GlobalScope.launch(Dispatchers.IO) { abcLogger.startAll() }
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            val ntf = Notifications.build(
                    context = this,
                    channelId = Notifications.CHANNEL_ID_FOREGROUND,
                    subText = getString(R.string.ntf_title_service_running),
                    removeViews = RemoteViews(packageName, R.layout.notification_foreground)
            )

            startForeground(Notifications.ID_FOREGROUND, ntf)

            return START_STICKY
        }
    }
}
