package kaist.iclab.abclogger.collector.sensor

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.AbstractEntity
import kaist.iclab.abclogger.commons.StringListConverter
import kaist.iclab.abclogger.commons.StringMapConverter


@Entity
data class EmbeddedSensorEntity(
        var valueType: String = "",
        @Convert(converter = StringMapConverter::class, dbType = String::class)
        var status: Map<String, String> = mapOf(),
        var valueFormat: String = "",
        var valueUnit: String = "",
        @Convert(converter = StringListConverter::class, dbType = String::class)
        var values: List<String> = listOf()
) : AbstractEntity()

/**
 * @param deviceType Type of sensor device; highest hierarchy of sensor entities (e.g., Smartphone, Polar H10)
 * @param valueType Type of sensor value; e.g., proximity, light, heart-rate
 * @param identifier Additional information that distinguishes different sensor (but having same type of devices).
 * @param status Optional description of current sensor status.
 * @param valueFormat Hints for original format of values.
 * @param values List of values as a form of String
 */
@Entity
data class ExternalSensorEntity(
        var deviceType: String = "",
        var valueType: String = "",
        var identifier: String = "",
        @Convert(converter = StringMapConverter::class, dbType = String::class)
        var status: Map<String, String> = mapOf(),
        var valueFormat: String = "",
        var valueUnit: String = "",
        @Convert(converter = StringListConverter::class, dbType = String::class)
        var values: List<String> = listOf()
) : AbstractEntity()