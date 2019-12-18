package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class PhysicalStatusEntity (
    var activity: String = "",
    var type: String = "",
    var startTime: Long = Long.MIN_VALUE,
    var endTime: Long = Long.MIN_VALUE,
    var value: Float = Float.MIN_VALUE
) : BaseEntity()