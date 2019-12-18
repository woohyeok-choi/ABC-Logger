package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity

import kaist.iclab.abclogger.data.types.BatteryPluggedType
import kaist.iclab.abclogger.data.types.BatteryPluggedTypeConverter
import kaist.iclab.abclogger.data.types.BatteryStatusType
import kaist.iclab.abclogger.data.types.BatteryStatusTypeConverter

/**
 * It represents a status of battery.
 *
 * @property id unique identifier in database
 * @property timestamp Unix timestamp in millis
 * @property utcOffset Offset time
 * @property level a current level of a battery power
 * @property temperature a current temperature of a battery
 * @property plugged a charging technology (e.g., AC, USB, etc.)
 * @property status a current status of a battery (e.g., FULL, DISCHARGING, CHARGING, etc.)
 */

@Entity
data class BatteryEntity (

    val level: Float = Float.MIN_VALUE,
    val temperature: Int = Int.MIN_VALUE,
    @Convert(converter = BatteryPluggedTypeConverter::class, dbType = String::class)
    val plugged: BatteryPluggedType = BatteryPluggedType.UNDEFINED,
    @Convert(converter = BatteryStatusTypeConverter::class, dbType = String::class)
    val status: BatteryStatusType = BatteryStatusType.UNDEFINED
) : BaseEntity ()