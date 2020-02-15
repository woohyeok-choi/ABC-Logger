package kaist.iclab.abclogger.collector

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id

@BaseEntity
abstract class Base(
        @Id var id: Long = 0,
        var timestamp: Long = -1,
        var utcOffset: Float = Float.MIN_VALUE,
        var subjectEmail: String = "",
        var deviceInfo: String = ""
)