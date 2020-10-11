package kaist.iclab.abclogger.collector.notification

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class NotificationEntity(
        var key: String = "",
        var groupKey: String = "",
        var notificationId: Int = Int.MIN_VALUE,
        var tag: String = "",
        var isClearable: Boolean = false,
        var isOngoing: Boolean = false,
        var name: String = "",
        var packageName: String = "",
        var postTime: Long = Long.MIN_VALUE,
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var title: String = "",
        var bigTitle: String = "",
        var text: String = "",
        var subText: String = "",
        var bigText: String = "",
        var summaryText: String = "",
        var infoText: String = "",
        var visibility: String = "",
        var category: String = "",
        var priority: String = "",
        var vibrate: String = "",
        var sound: String = "",
        var lightColor: String = "",
        var isPosted: Boolean = false
) : AbstractEntity()