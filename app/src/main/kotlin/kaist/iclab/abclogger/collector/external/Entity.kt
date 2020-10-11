package kaist.iclab.abclogger.collector.external

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.StringListConverter
import kaist.iclab.abclogger.commons.StringMapConverter

/**
 * @param deviceType Type of sensor device; highest hierarchy of sensor entities (e.g., Smartphone, Polar H10)
 * @param valueType Type of sensor value; e.g., proximity, light, heart-rate
 * @param identifier Additional information that distinguishes different sensor (but having same type of devices).
 * @param others Optional description of current sensor status.
 * @param valueFormat Hints for original format of values.
 * @param values List of values as a form of String
 */
@Entity
data class ExternalSensorEntity(
    var deviceType: String = "",
    var valueType: String = "",
    var identifier: String = "",
    @Convert(converter = StringMapConverter::class, dbType = String::class)
        var others: Map<String, String> = mapOf(),
    var valueFormat: String = "",
    var valueUnit: String = "",
    @Convert(converter = StringListConverter::class, dbType = String::class)
        var values: List<String> = listOf()
) : AbstractEntity()