package kaist.iclab.abclogger.collector.keylog

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.core.collector.AbstractEntity


@Entity
data class KeyLogEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var distance: Float = 0.0F,
        var timeTaken: Long = 0,
        var keyboardType: String = "",
        var prevKey: String = "",
        var currentKey: String = "",
        var prevKeyType: String = "",
        var currentKeyType: String = ""
) : AbstractEntity()