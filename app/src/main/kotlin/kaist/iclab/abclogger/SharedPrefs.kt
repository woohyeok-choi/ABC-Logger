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

    var subjectEmail: String by ReadWriteString()
    var participationTime: Long by ReadWriteLong(default = -1)
    var surveyJson : String by ReadWriteString()
    var lastTriggerTime : Map<String, Long>? by ReadWriteSerializable(type = inferType<Map<String, Long>>(), default = mapOf())

    var lastSyncTime: Long by ReadWriteLong(default = -1)
    var lastSmsAccessTime: Long by ReadWriteLong(default = -1)
    var lastMmsAccessTime: Long by ReadWriteLong(default = -1)
    var lastCallLogAccessTime: Long by ReadWriteLong(default = -1)
    var lastInternalPhotoAccessTime: Long by ReadWriteLong(default = -1)
    var lastInternalVideoAccessTime: Long by ReadWriteLong(default = -1)
    var lastExternalPhotoAccessTime: Long by ReadWriteLong(default = -1)
    var lastExternalVideoAccessTime: Long by ReadWriteLong(default = -1)
    var lastInstalledAppAccessTime: Long by ReadWriteLong(default = -1)
    var lastAppUsageAccessTime: Long by ReadWriteLong(default = -1)
    var lastPhysicalStatusAccessTime: Long by ReadWriteLong(default = -1)
    var lastSurveyTriggerTime1: Long by ReadWriteLong(default = -1)
    var lastSurveyTriggerTime2: Long by ReadWriteLong(default = -1)
    var lastSurveyTriggerTime3: Long by ReadWriteLong(default = -1)
    var lastSurveyTriggerTime4: Long by ReadWriteLong(default = -1)
    var lastSurveyTriggerTime5: Long by ReadWriteLong(default = -1)
}