package kaist.iclab.abclogger.collector.event

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity
import kaist.iclab.abclogger.commons.StringMapConverter

@Entity
data class DeviceEventEntity(
        var eventType: String = "",
        @Convert(converter = StringMapConverter::class, dbType = String::class)
        var extras: Map<String, String> = mapOf()
) : AbstractEntity()