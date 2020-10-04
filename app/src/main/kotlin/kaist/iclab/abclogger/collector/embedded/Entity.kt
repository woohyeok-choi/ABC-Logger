package kaist.iclab.abclogger.collector.embedded

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
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
