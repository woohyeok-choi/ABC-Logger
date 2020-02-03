package kaist.iclab.abclogger

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*
import github.agustarc.koap.inferType
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.collector.appusage.AppUsageCollector
import kaist.iclab.abclogger.collector.battery.BatteryCollector
import kaist.iclab.abclogger.collector.bluetooth.BluetoothCollector
import kaist.iclab.abclogger.collector.call.CallLogCollector
import kaist.iclab.abclogger.collector.event.DeviceEventCollector
import kaist.iclab.abclogger.collector.externalsensor.polar.PolarH10Collector
import kaist.iclab.abclogger.collector.install.InstalledAppCollector
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.location.LocationCollector
import kaist.iclab.abclogger.collector.media.MediaCollector
import kaist.iclab.abclogger.collector.message.MessageCollector
import kaist.iclab.abclogger.collector.notification.NotificationCollector
import kaist.iclab.abclogger.collector.physicalstat.PhysicalStatCollector
import kaist.iclab.abclogger.collector.internalsensor.SensorCollector
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.traffic.DataTrafficCollector
import kaist.iclab.abclogger.collector.wifi.WifiCollector

object PrefKeys {
    const val PERMISSION = "PERMISSION"
    const val LAST_TIME_SYNC = "LAST_TIME_SYNC"
    const val CAN_UPLOAD_METERED_NETWORK = "CAN_UPLOAD_METERED_NETWORK"
    const val MAX_DB_SIZE = "MAX_DB_SIZE"
    const val LOGOUT = "LOGOUT"

    const val STATUS_CALL_LOG = "STATUS_CALL_LOG"
    const val STATUS_MESSAGE = "STATUS_MESSAGE"
    const val STATUS_MEDIA = "STATUS_MEDIA"
    const val STATUS_BLUETOOTH = "STATUS_BLUETOOTH"
    const val STATUS_BATTERY = "STATUS_BATTERY"
    const val STATUS_WIFI = "STATUS_WIFI"
    const val STATUS_LOCATION = "STATUS_LOCATION"
    const val STATUS_ACTIVITY = "STATUS_ACTIVITY"
    const val STATUS_NOTIFICATION = "STATUS_NOTIFICATION"
    const val STATUS_APP_USAGE = "STATUS_APP_USAGE"
    const val STATUS_KEY_LOG = "STATUS_KEY_LOG"
    const val STATUS_DEVICE_EVENT = "STATUS_DEVICE_EVENT"
    const val STATUS_DATA_TRAFFIC = "STATUS_DATA_TRAFFIC"
    const val STATUS_INSTALLED_APP = "STATUS_INSTALLED_APP"
    const val STATUS_PHYSICAL_STAT = "STATUS_PHYSICAL_STAT"
    const val STATUS_SURVEY = "STATUS_SURVEY"
    const val STATUS_POLAR_H10 = "STATUS_POLAR_H10"
    const val STATUS_SENSOR = "STATUS_SENSOR"
}

object Prefs : PreferenceHolder(name = BuildConfig.PREF_NAME) {
    /**
     * Belows are presented to a user
     */
    var lastTimeDataSync: Long by ReadWriteLong(default = 0, key = PrefKeys.LAST_TIME_SYNC)
    var lastTimeDataFlush : Long by ReadWriteLong(default = 0)
    var canUploadMeteredNetwork: Boolean by ReadWriteBoolean(default = false, key = PrefKeys.CAN_UPLOAD_METERED_NETWORK)
    var maxDbSize : Long by ReadWriteLong(default = 0, key = PrefKeys.MAX_DB_SIZE)
    var statusCallLog: CallLogCollector.Status? by ReadWriteSerializable(type = inferType<CallLogCollector.Status>(), key = PrefKeys.STATUS_CALL_LOG)
    var statusMessage: MessageCollector.Status? by ReadWriteSerializable(type = inferType<MessageCollector.Status>(), key = PrefKeys.STATUS_MESSAGE)
    var statusMedia: MediaCollector.Status? by ReadWriteSerializable(type = inferType<MediaCollector.Status>(), key = PrefKeys.STATUS_MEDIA)
    var statusBluetooth: BluetoothCollector.Status? by ReadWriteSerializable(type = inferType<BluetoothCollector.Status>(), key = PrefKeys.STATUS_BLUETOOTH)
    var statusBattery: BatteryCollector.Status? by ReadWriteSerializable(type = inferType<BatteryCollector.Status>(), key = PrefKeys.STATUS_BATTERY)
    var statusWiFi: WifiCollector.Status? by ReadWriteSerializable(type = inferType<WifiCollector.Status>(), key = PrefKeys.STATUS_WIFI)
    var statusLocation: LocationCollector.Status? by ReadWriteSerializable(type = inferType<LocationCollector.Status>(), key = PrefKeys.STATUS_LOCATION)
    var statusActivity: ActivityCollector.Status? by ReadWriteSerializable(type = inferType<ActivityCollector.Status>(), key = PrefKeys.STATUS_ACTIVITY)
    var statusNotification: NotificationCollector.Status? by ReadWriteSerializable(type = inferType<NotificationCollector.Status>(), key = PrefKeys.STATUS_NOTIFICATION)
    var statusAppUsage: AppUsageCollector.Status? by ReadWriteSerializable(type = inferType<AppUsageCollector.Status>(), key = PrefKeys.STATUS_APP_USAGE)
    var statusKeyLog: KeyLogCollector.Status? by ReadWriteSerializable(type = inferType<KeyLogCollector.Status>(), key = PrefKeys.STATUS_KEY_LOG)
    var statusDeviceEvent: DeviceEventCollector.Status? by ReadWriteSerializable(type = inferType<DeviceEventCollector.Status>(), key = PrefKeys.STATUS_DEVICE_EVENT)
    var statusDataTraffic: DataTrafficCollector.Status? by ReadWriteSerializable(type = inferType<DataTrafficCollector.Status>(), key = PrefKeys.STATUS_DATA_TRAFFIC)
    var statusInstallApp: InstalledAppCollector.Status? by ReadWriteSerializable(type = inferType<InstalledAppCollector.Status>(), key = PrefKeys.STATUS_INSTALLED_APP)
    var statusPhysicalStat: PhysicalStatCollector.Status? by ReadWriteSerializable(type = inferType<PhysicalStatCollector.Status>(), key = PrefKeys.STATUS_PHYSICAL_STAT)
    var statusSurvey: SurveyCollector.Status? by ReadWriteSerializable(type = inferType<SurveyCollector.Status>(), key = PrefKeys.STATUS_SURVEY)
    var statusPolarH10: PolarH10Collector.Status? by ReadWriteSerializable(type = inferType<PolarH10Collector.Status>(), key = PrefKeys.STATUS_POLAR_H10)
    var statusSensor: SensorCollector.Status? by ReadWriteSerializable(type = inferType<SensorCollector.Status>(), key = PrefKeys.STATUS_SENSOR)
    /**
     * Belows are not presented to a user
     */
    var dbVersion : Int by ReadWriteInt(default = 0)
}
