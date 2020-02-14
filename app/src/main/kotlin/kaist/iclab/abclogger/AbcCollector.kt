package kaist.iclab.abclogger

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import github.agustarc.koap.Koap
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.commons.Notifications
import kaist.iclab.abclogger.commons.checkServiceRunning
import kaist.iclab.abclogger.ui.AvoidSmartManagerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*
import kotlin.random.Random

class AbcCollector(vararg collector: BaseCollector<*>) {
    val collectors = arrayOf(*collector)

    suspend fun startAll() {
        collectors.forEach {
            if (it.getStatus()?.hasStarted == true) it.start()
        }
    }

    suspend fun stopAll() {
        collectors.forEach {
            if (it.getStatus()?.hasStarted == true) it.stop()
        }
    }

    fun getAllRequiredPermissions() = collectors.map { it.requiredPermissions }.flatten()

    class LoggerService : Service() {
        private val abcLogger: AbcCollector by inject()

        override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
            GlobalScope.launch(Dispatchers.IO) { abcLogger.startAll() }
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
    }


    class BootReceiver : BroadcastReceiver() {
        companion object {
            const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"
        }

        override fun onReceive(context: Context, intent: Intent) {
            val timestamp = System.currentTimeMillis()
            val action = intent.action?.toLowerCase(Locale.getDefault()) ?: return
            val filters = listOf(
                    "android.intent.action.QUICKBOOT_POWERON",
                    Intent.ACTION_BOOT_COMPLETED
            ).map { it.toLowerCase(Locale.getDefault()) }

            if (action !in filters) return

            try {
                context.packageManager.getPackageInfo(PACKAGE_NAME_SMART_MANAGER, PackageManager.GET_META_DATA)

                Handler().postDelayed({
                    context.startActivity(Intent(context, AvoidSmartManagerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }, Random.nextLong(3000))
            } catch (e: PackageManager.NameNotFoundException) { }

            start(context)
            AbcEvent.post(timestamp, AbcEvent.BOOT_COMPLETED)
        }
    }

    companion object {
        fun start(context: Context) {
            if(!checkServiceRunning<LoggerService>(context) && FirebaseAuth.getInstance().currentUser != null)
                ContextCompat.startForegroundService(context, Intent(context, LoggerService::class.java))
        }

        fun stop(context: Context) {
            if(checkServiceRunning<LoggerService>(context))
                context.stopService(Intent(context, LoggerService::class.java))
        }

        fun bind(context: Context) {
            Notifications.bind(context)
            Koap.bind(context, Prefs)
            ObjBox.bind(context)
        }
    }
}
