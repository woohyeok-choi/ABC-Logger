package kaist.iclab.abclogger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import kaist.iclab.abclogger.sync.SyncManager
import kaist.iclab.abclogger.ui.AvoidSmartManagerActivity
import java.util.*


class BootReceiver : BroadcastReceiver() {
    companion object {
        const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.toLowerCase(Locale.getDefault()) ?: return
        val filters = listOf(
                "android.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_BOOT_COMPLETED
        ).map { it.toLowerCase(Locale.getDefault()) }

        if (action in filters) {
            try {
                context.packageManager.getPackageInfo(PACKAGE_NAME_SMART_MANAGER, PackageManager.GET_META_DATA)

                Handler().postDelayed({
                    context.startActivity(Intent(context, AvoidSmartManagerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }, Random(System.currentTimeMillis()).nextInt(3000).toLong())
            } catch (e: PackageManager.NameNotFoundException) { }

            context.startForegroundService<ABCLogger.ABCLoggerService>()
        }
    }
}
