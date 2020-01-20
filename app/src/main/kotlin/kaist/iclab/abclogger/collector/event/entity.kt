package kaist.iclab.abclogger.collector.event

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.Base

@Entity
data class DeviceEventEntity(
        var type: String = ""
) : Base()