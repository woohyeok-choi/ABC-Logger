package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class SensorEntity(
    var type: String = "",
    var firstValue: Float = Float.MIN_VALUE,
    var secondValue: Float = Float.MIN_VALUE,
    var thirdValue: Float = Float.MIN_VALUE,
    var fourthValue: Float = Float.MIN_VALUE
) : BaseEntity()