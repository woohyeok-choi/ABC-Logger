package kaist.iclab.abclogger.collector.externalsensor

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.Base

@Entity
data class ExternalSensorEntity(
        var sensorId: String = "",
        var name: String = "",
        var description: String = "",
        var firstValue: String = "",
        var secondValue: String = "",
        var thirdValue: String = "",
        var fourthValue: String = ""
) : Base()
