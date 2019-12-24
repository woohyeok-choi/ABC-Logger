package kaist.iclab.abclogger.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.common.util.Utils
import kaist.iclab.abclogger.data.entities.DeviceEvent
import kaist.iclab.abclogger.data.types.DeviceEventType
import kaist.iclab.abclogger.foreground.activity.AvoidSmartManagerActivity
import kaist.iclab.abclogger.prefs
import java.util.*


class BootReceiver: BroadcastReceiver() {
    companion object {
        const val PACKAGE_NAME_SMART_MANAGER = "com.samsung.android.sm"
    }

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ) {
            //val experiment = try { ParticipationEntity.getParticipatedExperimentFromLocal() } catch (e: Exception) { null }
            //if(experiment != null) {
            if (prefs.participantSignedIn) {
                val entity = DeviceEvent(DeviceEventType.TURN_ON_DEVICE).apply {
                    timestamp = System.currentTimeMillis()
                    utcOffset = Utils.utcOffsetInHour()
                    subjectEmail = prefs.participantEmail!!
                    experimentUuid = prefs.participantPhoneNumber!!
                    experimentGroup = prefs.participantGroup!!
                }
                App.boxFor<DeviceEvent>().put( entity )

                /* Boot - SW EDIT */
                //val gson = GsonBuilder().setPrettyPrinting().create()
                //val jsonEntity: String = gson.toJson(entity)
                //MySQLiteLogger.writeStringData(context, entity.javaClass.simpleName, entity.timestamp, jsonEntity)

            }

            SyncManager.sync(true)

            try {
                context.packageManager.getPackageInfo(PACKAGE_NAME_SMART_MANAGER, PackageManager.GET_META_DATA)

                Handler().postDelayed( {
                    context.startActivity(Intent(context, AvoidSmartManagerActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }, Random(System.currentTimeMillis()).nextInt(3000).toLong())
            } catch (e: PackageManager.NameNotFoundException) { }
        }
    }
}
