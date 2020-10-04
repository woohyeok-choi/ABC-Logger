package kaist.iclab.abclogger.collector.call

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

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
