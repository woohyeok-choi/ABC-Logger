package kaist.iclab.abclogger.ui.config

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.collector.*
import kaist.iclab.abclogger.ui.LoadViewModel
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

class ConfigViewModel(
        val context: Context,
        val activityCollector: ActivityCollector,
        val appUsageCollector: AppUsageCollector,
        val batteryCollector: BatteryCollector,
        val bluetoothCollector: BluetoothCollector,
        val callLogCollector: CallLogCollector,
        val dataTrafficCollector: DataTrafficCollector,
        val deviceEventCollector: DeviceEventCollector,
        val installedAppCollector: InstalledAppCollector,
        val keyTrackingService: KeyTrackingService,
        val locationCollector: LocationCollector,
        val mediaCollector: MediaCollector,
        val messageCollector: MessageCollector,
        val notificationCollector: NotificationCollector,
        val physicalStatusCollector: PhysicalStatusCollector,
        val polarH10Collector: PolarH10Collector,
        val surveyCollector: SurveyCollector,
        val wifiCollector: WifiCollector
) : LoadViewModel<ConfigData>() {
    override fun loadData(): ConfigData {
        val lastSyncTime = SharedPrefs.lastTimeDataSync
        val sizeOfDb = ObjBox.size(context)
        val a = ActivityCollector::class.companionObjectInstance
        if (a is test) {
            a.show()
        }


        return ConfigData(
                lastSyncTime = lastSyncTime,
                sizeOfDb = sizeOfDb
        )
    }

    interface test {
        fun show()
    }
}