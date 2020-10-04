package kaist.iclab.abclogger.core

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.commons.isServiceRunning
import kaist.iclab.abclogger.ui.AvoidSmartManagerActivity
import org.koin.android.ext.android.inject
import kotlin.random.Random

/**
 * TODO: Private set check
 *
 */

class AbcCollector(vararg collector: BaseCollector) {
    val collectors = collector

    val permissions by lazy {
        collectors.map { it.permissions }.flatten().toSet()
    }

    fun start() = collectors.filter { it.isEnabled }.forEach { it.start() }

    fun stop() = collectors.forEach { it.stop() }

    inline fun <reified T : BaseCollector> get(): T? = collectors.filterIsInstance<T>().firstOrNull()

    @Suppress("UNCHECKED_CAST")
    fun <T : BaseCollector> get(qualifiedName: String): T? = collectors.find { it.qualifiedName == qualifiedName } as? T

    class ForegroundService : Service() {
        private val abcCollector: AbcCollector by inject()

        override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
            abcCollector.start()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            abcCollector.get<DeviceEventCollector>()?.writeBootEvent(intent)

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
        override fun onReceive(context: Context, intent: Intent) {
            try {
                context.packageManager.getPackageInfo("com.samsung.android.sm", PackageManager.GET_META_DATA)

                Handler(Looper.getMainLooper()).postDelayed({
                    context.startActivity(Intent(context, AvoidSmartManagerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }, Random.nextLong(3000))
            } catch (e: Exception) {

            }

            start(context, System.currentTimeMillis(), intent.action)
        }
    }

    /**
     * TODO: PowerManager Setting Check - Location etc.
     * TODO: JobScheduler can be used to send heartbeats and check collector status
     */
    companion object {
        fun start(context: Context, timestamp: Long, action: String? = null) {
            if (isServiceRunning<ForegroundService>(context)) return
            if (FirebaseAuth.getInstance().currentUser == null) return

            val intent = DeviceEventCollector.fillBootEvent(
                    Intent(context, ForegroundService::class.java), timestamp, action
            )
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            if (!isServiceRunning<ForegroundService>(context)) return

            context.stopService(Intent(context, ForegroundService::class.java))
        }
    }
}
