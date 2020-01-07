package kaist.iclab.abclogger

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*
import github.agustarc.koap.inferType

object SharedPrefs : PreferenceHolder(name = "abc-logger-pref") {
    var isProvidedCallLog: Boolean by ReadWriteBoolean(default = false)
    var isProvidedMessage: Boolean by ReadWriteBoolean(default = false)
    var isProvidedMediaGeneration: Boolean by ReadWriteBoolean(default = false)
    var isProvidedBluetooth: Boolean by ReadWriteBoolean(default = false)
    var isProvidedBattery: Boolean by ReadWriteBoolean(default = false)
    var isProvidedWiFi: Boolean by ReadWriteBoolean(default = false)
    var isProvidedLocation: Boolean by ReadWriteBoolean(default = false)
    var isProvidedActivity: Boolean by ReadWriteBoolean(default = false)
    var isProvidedNotification: Boolean by ReadWriteBoolean(default = false)
    var isProvidedAppUsage: Boolean by ReadWriteBoolean(default = false)
    var isProvidedKeyStrokes: Boolean by ReadWriteBoolean(default = false)
    var isProvidedDeviceEvent: Boolean by ReadWriteBoolean(default = false)
    var isProvidedDataTraffic: Boolean by ReadWriteBoolean(default = false)
    var isProvidedInstallApp: Boolean by ReadWriteBoolean(default = false)
    var isProvidedPhysicalStatus: Boolean by ReadWriteBoolean(default = false)
    var isProvidedSurvey: Boolean by ReadWriteBoolean(default = false)
    var isProvidedPolarH10 : Boolean by ReadWriteBoolean(default = false)

    var statusCallLog: String by ReadWriteString()
    var statusMessage: String by ReadWriteString()
    var statusMediaGeneration: String by ReadWriteString()
    var statusBluetooth: String by ReadWriteString()
    var statusBattery: String by ReadWriteString()
    var statusWiFi: String by ReadWriteString()
    var statusLocation: String by ReadWriteString()
    var statusActivity: String by ReadWriteString()
    var statusNotification: String by ReadWriteString()
    var statusAppUsage: String by ReadWriteString()
    var statusKeyStrokes: String by ReadWriteString()
    var statusDeviceEvent: String by ReadWriteString()
    var statusDataTraffic: String by ReadWriteString()
    var statusInstallApp: String by ReadWriteString()
    var statusPhysicalStatus: String by ReadWriteString()
    var statusSurvey: String by ReadWriteString()
    var statusPolarH10: String by ReadWriteString()

    var subjectEmail: String by ReadWriteString()
    var participationTime: Long by ReadWriteLong(default = -1)

    var lastTimeDataSync: Long by ReadWriteLong(default = -1)

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
}

object ExternalDevicePrefs : PreferenceHolder(name = "abc-logger-ext-device-pref") {
    var polarH10DeviceId: String by ReadWriteString()
    var polarH10Connection: String by ReadWriteString("DISCONNECTED")
    var polarH10BatteryLevel: Int by ReadWriteInt(default = -1)
    var polarH10HrFeatureReady: Boolean by ReadWriteBoolean(default = false)
    var polarH10EcgFeatureReady: Boolean by ReadWriteBoolean(default = false)
    var polarH10Exception: String by ReadWriteString()
}