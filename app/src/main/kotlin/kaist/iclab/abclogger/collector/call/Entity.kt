package kaist.iclab.abclogger.collector.content

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.AbstractEntity

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
) : AbstractEntity()

@Entity
data class MessageEntity(
        var number: String = "",
        var messageClass: String = "",
        var messageBox: String = "",
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : AbstractEntity()

@Entity
data class MediaEntity(
        var mimeType: String = ""
) : AbstractEntity()