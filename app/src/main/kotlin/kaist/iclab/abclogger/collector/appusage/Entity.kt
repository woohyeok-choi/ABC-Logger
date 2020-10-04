package kaist.iclab.abclogger.collector.appusage

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class AppUsageEventEntity(
        var name: String = "",
        var packageName: String = "",
        var type: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false
) : AbstractEntity()