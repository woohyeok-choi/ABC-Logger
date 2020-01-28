package kaist.iclab.abclogger.collector.physicalstatus

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class PhysicalStatusEntity(
        var type: String = "",
        var startTime: Long = 0,
        var endTime: Long = 0,
        var value: Float = Float.MIN_VALUE
) : Base()
