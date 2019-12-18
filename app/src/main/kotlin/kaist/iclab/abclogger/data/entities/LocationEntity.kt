package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

/**
 * It represents a user'transitionScanClient current location
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property latitude latitude in GPS
 * @property longitude longitude in GPS
 * @property altitude altitude in GPS
 * @property accuracy accuracy in GPS
 * @property speed speed
 */

@Entity
data class LocationEntity (
    var latitude: Double = Double.MIN_VALUE,
    var longitude: Double = Double.MIN_VALUE,
    var altitude: Double = Double.MIN_VALUE,
    var accuracy: Float = Float.MIN_VALUE,
    var speed: Float = Float.MIN_VALUE
) : BaseEntity()