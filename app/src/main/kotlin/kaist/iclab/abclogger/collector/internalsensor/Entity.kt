package kaist.iclab.abclogger.collector.internalsensor

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class SensorEntity(
        var type: String = "",
        var accuracy: String = "",
        var firstValue: Float = Float.MIN_VALUE,
        var secondValue: Float = Float.MIN_VALUE,
        var thirdValue: Float = Float.MIN_VALUE,
        var fourthValue: Float = Float.MIN_VALUE
) : Base()
