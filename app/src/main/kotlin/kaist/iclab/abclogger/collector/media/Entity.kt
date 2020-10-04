package kaist.iclab.abclogger.collector.media

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity

@Entity
data class MediaEntity(
        var mimeType: String = ""
) : AbstractEntity()