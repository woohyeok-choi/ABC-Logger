package kaist.iclab.abclogger

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*

object GeneralPrefs : PreferenceHolder(name = BuildConfig.PREF_NAME_GENERAL) {
    var participationTime: Long by ReadWriteLong(default = -1)
    var lastTimeDataSync: Long by ReadWriteLong(default = -1)
    var shouldUploadForNonMeteredNetwork: Boolean by ReadWriteBoolean(default = false)
    var dbVersion : Int by ReadWriteInt(default = 0)
}

object CollectorPrefs : PreferenceHolder(name = BuildConfig.PREF_NAME_COLLECTOR) {
    var hasStartedCallLog: Boolean by ReadWriteBoolean(default = false)
    var hasStartedMessage: Boolean by ReadWriteBoolean(default = false)
    var hasStartedMediaGeneration: Boolean by ReadWriteBoolean(default = false)
    var hasStartedBluetooth: Boolean by ReadWriteBoolean(default = false)
    var hasStartedBattery: Boolean by ReadWriteBoolean(default = false)
    var hasStartedWiFi: Boolean by ReadWriteBoolean(default = false)
    var hasStartedLocation: Boolean by ReadWriteBoolean(default = false)
    var hasStartedActivity: Boolean by ReadWriteBoolean(default = false)
    var hasStartedNotification: Boolean by ReadWriteBoolean(default = false)
    var hasStartedAppUsage: Boolean by ReadWriteBoolean(default = false)
    var hasStartedKeyStrokes: Boolean by ReadWriteBoolean(default = false)
    var hasStartedDeviceEvent: Boolean by ReadWriteBoolean(default = false)
    var hasStartedDataTraffic: Boolean by ReadWriteBoolean(default = false)
    var hasStartedInstallApp: Boolean by ReadWriteBoolean(default = false)
    var hasStartedPhysicalStatus: Boolean by ReadWriteBoolean(default = false)
    var hasStartedSurvey: Boolean by ReadWriteBoolean(default = false)
    var hasStartedPolarH10: Boolean by ReadWriteBoolean(default = false)
    var hasStartedSensor: Boolean by ReadWriteBoolean(default = false)

    var infoCallLog: String by ReadWriteString()
    var infoMessage: String by ReadWriteString()
    var infoMediaGeneration: String by ReadWriteString()
    var infoBluetooth: String by ReadWriteString()
    var infoBattery: String by ReadWriteString()
    var infoWiFi: String by ReadWriteString()
    var infoLocation: String by ReadWriteString()
    var infoActivity: String by ReadWriteString()
    var infoNotification: String by ReadWriteString()
    var infoAppUsage: String by ReadWriteString()
    var infoKeyStrokes: String by ReadWriteString()
    var infoDeviceEvent: String by ReadWriteString()
    var infoDataTraffic: String by ReadWriteString()
    var infoInstallApp: String by ReadWriteString()
    var infoPhysicalStatus: String by ReadWriteString()
    var infoSurvey: String by ReadWriteString()
    var infoPolar: String by ReadWriteString()
    var infoSensor : String by ReadWriteString()

    var lastAccessTimeSms: Long by ReadWriteLong(default = -1)
    var lastAccessTimeMms: Long by ReadWriteLong(default = -1)
    var lastAccessTimeCallLog: Long by ReadWriteLong(default = -1)
    var lastAccessTimeInternalPhoto: Long by ReadWriteLong(default = -1)
    var lastAccessTimeInternalVideo: Long by ReadWriteLong(default = -1)
    var lastAccessTimeExternalPhoto: Long by ReadWriteLong(default = -1)
    var lastAccessTimeExternalVideo: Long by ReadWriteLong(default = -1)
    var lastAccessTimeInstalledApp: Long by ReadWriteLong(default = -1)
    var lastAccessTimeAppUsage: Long by ReadWriteLong(default = -1)
    var lastAccessTimePhysicalStatus: Long by ReadWriteLong(default = -1)

    var softKeyboardType: String by ReadWriteString()

    var polarH10DeviceId: String by ReadWriteString()
    var polarH10Connection: Int by ReadWriteInt(default = 0)
    var polarH10BatteryLevel: Int by ReadWriteInt(default = -1)
}
