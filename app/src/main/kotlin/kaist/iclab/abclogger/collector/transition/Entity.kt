package kaist.iclab.abclogger.collector.transition

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class PhysicalActivityTransitionEntity(
        var type: String = "",
        var isEntered: Boolean = false
) : AbstractEntity()