package kaist.iclab.abclogger.collector.message

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class MessageEntity (
        var number: String = "",
        var messageClass: String = "",
        var messageBox: String = "",
        var contactType: String = "",
        var isStarred: Boolean = false,
        var isPinned: Boolean = false
) : Base()