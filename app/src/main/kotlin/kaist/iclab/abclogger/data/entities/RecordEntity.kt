package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class RecordEntity(
        var sampleRate: Int = Int.MIN_VALUE,
        var channelMask: String = "",
        var encoding: String = "",
        var path: String = "",
        var duration: Long = Long.MIN_VALUE
) : BaseEntity()