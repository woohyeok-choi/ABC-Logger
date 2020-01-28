package kaist.iclab.abclogger.collector.event

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.collector.Base

@Entity
data class DeviceEventEntity(
        var type: String = ""
) : Base()