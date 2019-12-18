package kaist.iclab.abclogger

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.ReadWriteBoolean
import github.agustarc.koap.delegator.ReadWriteLong
import github.agustarc.koap.delegator.ReadWriteString

object SharedPrefs : PreferenceHolder(name = "abc-logger-pref") {
    var isProvidedCallLog: Boolean by ReadWriteBoolean(default = false)
    var isProvidedMessage: Boolean by ReadWriteBoolean(default = false)
    var isProvidedMediaGeneration: Boolean by ReadWriteBoolean(default = false)
    var isProvidedBluetooth: Boolean by ReadWriteBoolean(default = false)
    var isProvidedWiFi: Boolean by ReadWriteBoolean(default = false)
    var isProvidedLocation: Boolean by ReadWriteBoolean(default = false)
    var isProvidedNotification: Boolean by ReadWriteBoolean(default = false)
    var isProvidedAppUsage: Boolean by ReadWriteBoolean(default = false)
    var isProvidedKeyStrokes: Boolean by ReadWriteBoolean(default = false)
    var isProvidedDeviceEvent: Boolean by ReadWriteBoolean(default = false)
    var isProvidedDataFraffic: Boolean by ReadWriteBoolean(default = false)
    var isProvidedGoogleFitness: Boolean by ReadWriteBoolean(default = false)

    var userId: String by ReadWriteString()
    var userGroup: String by ReadWriteString()
    var experimentName: String by ReadWriteString()
    var experimentStartTime: Long by ReadWriteLong(default = -1)
    var lastSyncTime: Long by ReadWriteLong(default = -1)

    var lastMessageAccessTime: Long by ReadWriteLong(default = -1)
    var lastCallLogAccessTime: Long by ReadWriteLong(default = -1)
    var lastMediaAccessTime: Long by ReadWriteLong(default = -1)
}