package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.BaseEntity
import io.objectbox.annotation.Id

@BaseEntity
abstract class BaseEntity (@Id var id: Long = 0,
                           var timestamp: Long = Long.MIN_VALUE,
                           var utcOffset: Float = Float.MIN_VALUE,
                           var subjectEmail: String = "",
                           var experimentUuid: String = "",
                           var experimentGroup: String = "",
                           var isUploaded: Boolean = false)