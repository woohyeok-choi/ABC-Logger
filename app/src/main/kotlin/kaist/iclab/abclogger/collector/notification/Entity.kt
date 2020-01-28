package kaist.iclab.abclogger.collector.notification

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

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
        var isPosted: Boolean = false
) : Base()