package kaist.iclab.abclogger.collector.location

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class LocationEntity(
        var latitude: Double = Double.MIN_VALUE,
        var longitude: Double = Double.MIN_VALUE,
        var altitude: Double = Double.MIN_VALUE,
        var accuracy: Float = Float.MIN_VALUE,
        var speed: Float = Float.MIN_VALUE
) : AbstractEntity()