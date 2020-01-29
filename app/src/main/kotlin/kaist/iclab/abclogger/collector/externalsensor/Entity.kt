package kaist.iclab.abclogger.collector.externalsensor

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class ExternalSensorEntity(
        var sensorId: String = "",
        var name: String = "",
        var description: String = "",
        var firstValue: Float = 0F,
        var secondValue: Float = 0F,
        var thirdValue: Float = 0F,
        var fourthValue: Float = 0F
) : Base()
