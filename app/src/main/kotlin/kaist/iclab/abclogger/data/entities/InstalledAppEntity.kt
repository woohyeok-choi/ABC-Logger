package kaist.iclab.abclogger.data.entities

import io.objectbox.annotation.Entity

@Entity
data class InstalledAppEntity (
    var name: String = "",
    var packageName: String = "",
    var isSystemApp: Boolean = false,
    var isUpdatedSystemApp: Boolean = false,
    var firstInstallTime : Long = Long.MIN_VALUE,
    var lastUpdateTime: Long = Long.MIN_VALUE
) : BaseEntity()