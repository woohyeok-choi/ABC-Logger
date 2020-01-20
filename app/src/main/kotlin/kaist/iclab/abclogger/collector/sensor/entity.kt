package kaist.iclab.abclogger.collector.sensor

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.Base

@Entity
data class SensorEntity(
        var sensorId: String = "",
        var sensorName: String = "",
        var valueDescription: String = "",
        var valueType: String = "",
        var firstValue: Float = Float.MIN_VALUE,
        var secondValue: Float = Float.MIN_VALUE,
        var thirdValue: Float = Float.MIN_VALUE,
        var fourthValue: Float = Float.MIN_VALUE
) : Base()
