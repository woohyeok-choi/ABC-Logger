package kaist.iclab.abclogger.core

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id

@BaseEntity
abstract class AbstractEntity(
        @Id var id: Long = 0,
        var timestamp: Long = Long.MIN_VALUE,
        var utcOffset: Int = Int.MIN_VALUE,
        var email: String = "",
        var deviceInfo: String = "",
        var deviceId: String = ""
)