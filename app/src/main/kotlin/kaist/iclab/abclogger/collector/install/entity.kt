package kaist.iclab.abclogger.collector.install

import io.objectbox.annotation.Entity
import kaist.iclab.abclogger.Base


@Entity
data class InstalledAppEntity(
        var name: String = "",
        var packageName: String = "",
        var isSystemApp: Boolean = false,
        var isUpdatedSystemApp: Boolean = false,
        var firstInstallTime: Long = Long.MIN_VALUE,
        var lastUpdateTime: Long = Long.MIN_VALUE
) : Base()
