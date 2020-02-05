package kaist.iclab.abclogger.collector.externalsensor

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class ExternalSensorEntity(
        var sensorId: String = "",
        var name: String = "",
        var description: String = "",
        var firstValue: Float = Float.MIN_VALUE,
        var secondValue: Float = Float.MIN_VALUE,
        var thirdValue: Float = Float.MIN_VALUE,
        var fourthValue: Float = Float.MIN_VALUE
) : Base()
