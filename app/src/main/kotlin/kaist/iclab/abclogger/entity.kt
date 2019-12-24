package kaist.iclab.abclogger

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import kaist.iclab.abclogger.survey.SurveyTimeoutPolicyType
import java.util.*

@BaseEntity
abstract class Base(
        @Id var id: Long = 0,
        var timestamp: Long = -1,
        var utcOffset: Float = Float.MIN_VALUE,
        var subjectEmail: String = "",
        var participationTime: Long = -1,
        var isUploaded: Boolean = false
)

fun <T : Base> T.fillBaseInfo(timestamp: Long): T {
    this.timestamp = timestamp
    this.utcOffset = TimeZone.getDefault().rawOffset.toFloat() / (1000 * 60 * 60)
    this.subjectEmail = SharedPrefs.subjectEmail
    this.participationTime = SharedPrefs.participationTime
    this.isUploaded = false

    return this
}

@Entity
data class AppUsageEventEntity(
        var name: String = "",
        var packageName: String = "",
        var type: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false
) : Base()

@Entity
data class BatteryEntity(
        var level: Int = Int.MIN_VALUE,
        var scale: Int = Int.MIN_VALUE,
        var temperature: Int = Int.MIN_VALUE,
        var voltage: Int = Int.MIN_VALUE,
        var health: String = "",
        var pluggedType: String = "",
        var status: String = ""
) : Base()

@Entity
data class BluetoothEntity(
        var deviceName: String = "",
        var address: String = "",
        var rssi: Int = Int.MIN_VALUE,
) : Base()

@Entity
data class CallLogEntity(
        var duration: Long = Long.MIN_VALUE,
        var number: String = "",
        var type: String = "",
        var presentation: String = "",
        var dataUsage: Long = Long.MIN_VALUE,
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : Base()

@Entity
data class DataTrafficEntity(
        var fromTime: Long = Long.MIN_VALUE,
        var rxBytes: Long = Long.MIN_VALUE,
        var txBytes: Long = Long.MIN_VALUE,
        var mobileRxBytes: Long = Long.MIN_VALUE,
        var mobileTxBytes: Long = Long.MIN_VALUE
) : Base()


@Entity
data class DeviceEventEntity(
        var type: String = ""
) : Base()

@Entity
data class InstalledAppEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var firstInstallTime: Long = Long.MIN_VALUE,
        var lastUpdateTime: Long = Long.MIN_VALUE
) : Base()

@Entity
data class LocationEntity(
        var latitude: Double = Double.MIN_VALUE,
        var longitude: Double = Double.MIN_VALUE,
        var altitude: Double = Double.MIN_VALUE,
        var accuracy: Float = Float.MIN_VALUE,
        var speed: Float = Float.MIN_VALUE
) : Base()

@Entity
data class MediaEntity(
        var mimeType: String = "",
        var bucketDisplay: String = ""
) : Base()

@Entity
data class NotificationEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var title: String = "",
        var visibility: String = "",
        var category: String = "",
        var vibrate: String = "",
        var sound: String = "",
        var lightColor: String = "",
        var isRemoved: Boolean = false
) : Base()

@Entity
data class PhysicalActivityTransitionEntity(
        var type: String = "",
        var isEntered: Boolean = true
) : Base()

@Entity
data class PhysicalActivityEntity(
        var type: String = "",
        var confidence: Int = -1
) : Base()

@Entity
data class PhysicalStatusEntity(
        var type: String = "",
        var startTime: Long = Long.MIN_VALUE,
        var endTime: Long = Long.MIN_VALUE,
        var value: Float = Float.MIN_VALUE
) : Base()

@Entity
data class SurveyEntity(
        var title: String = "",
        var message: String = "",
        var timeoutPolicy: String = "",
        var timeoutSec: Long = Long.MIN_VALUE
        var deliveredTime: Long = Long.MIN_VALUE,
        var reactionTime: Long = Long.MIN_VALUE,
        var firstQuestionTime: Long = Long.MIN_VALUE,
        var responseTime: Long = Long.MIN_VALUE,
        var json: String = ""
) : Base()

@Entity
data class RecordEntity(
        var sampleRate: Int = Int.MIN_VALUE,
        var channelMask: String = "",
        var encoding: String = "",
        var path: String = "",
        var duration: Long = Long.MIN_VALUE
) : Base()

@Entity
data class WifiEntity(
        var bssid: String = "",
        var ssid: String = "",
        var frequency: Int = Int.MIN_VALUE,
        var rssi: Int = Int.MIN_VALUE
) : Base()


@Entity
data class MessageEntity (
        var number: String = "",
        var messageClass: String = "",
        var messageBox: String = "",
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : Base()

@Entity
data class SensorEntity(
        var type: String = "",
        var firstValue: Float = Float.MIN_VALUE,
        var secondValue: Float = Float.MIN_VALUE,
        var thirdValue: Float = Float.MIN_VALUE,
        var fourthValue: Float = Float.MIN_VALUE
) : Base()