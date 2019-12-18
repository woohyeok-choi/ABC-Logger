package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class AppUsageStatEntity (
    var name: String = "",
    var packageName: String = "",
    var isSystemApp: Boolean = false,
    var isUpdatedSystemApp: Boolean = false,
    var startTime: Long = Long.MIN_VALUE,
    var endTime: Long = Long.MIN_VALUE,
    var lastTimeUsed: Long = Long.MIN_VALUE,
    var totalTimeForeground: Long = Long.MIN_VALUE
) : BaseEntity()