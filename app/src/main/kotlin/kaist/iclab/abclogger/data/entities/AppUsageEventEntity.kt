package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity

import kaist.iclab.abclogger.data.types.AppUsageEventType
import kaist.iclab.abclogger.data.types.AppUsageEventTypeConverter


@Entity
data class AppUsageEventEntity (
    var name: String = "",
    var packageName: String = "",
    @Convert(converter = AppUsageEventTypeConverter::class, dbType = String::class)
    var type: AppUsageEventType = AppUsageEventType.NONE,
    var isSystemApp: Boolean = false,
    var isUpdatedSystemApp: Boolean = false
) : BaseEntity()