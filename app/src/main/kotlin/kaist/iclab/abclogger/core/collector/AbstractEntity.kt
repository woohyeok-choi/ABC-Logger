package kaist.iclab.abclogger.core.collector

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id

@BaseEntity
abstract class AbstractEntity(
        @Id var id: Long = 0,
        var timestamp: Long = Long.MIN_VALUE,
        var utcOffset: Int = Int.MIN_VALUE,
        var groupName: String = "",
        var email: String = "",
        var instanceId: String = "",
        var source: String = "",
        var deviceManufacturer: String = "",
        var deviceModel: String = "",
        var deviceVersion: String = "",
        var deviceOs: String = "",
        var appId: String = "",
        var appVersion: String = "",

)