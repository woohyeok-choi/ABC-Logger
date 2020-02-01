package kaist.iclab.abclogger.collector.activity

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class PhysicalActivityTransitionEntity(
        var type: String = "",
        var isEntered: Boolean = false
) : Base()

@Entity
data class PhysicalActivityEntity(
        var type: String = "",
        var confidence: Int = Int.MIN_VALUE
) : Base()
