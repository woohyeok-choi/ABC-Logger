package kaist.iclab.abclogger

import android.content.Context
import android.content.Intent
import android.os.IBinder
import github.agustarc.koap.Koap
import kaist.iclab.abclogger.base.BaseService

class ABCLogger : BaseService() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        fun bind(context: Context) {
            ObjBox.bind(context)
            Koap.bind(context, SharedPrefs, ExternalDevicePrefs)
            Notifications.bind(context)
        }
    }
}