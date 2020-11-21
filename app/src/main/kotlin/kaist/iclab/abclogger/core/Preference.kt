package kaist.iclab.abclogger.core

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*
import kaist.iclab.abclogger.BuildConfig

object Preference : PreferenceHolder(name = "${BuildConfig.APPLICATION_ID}.Preference") {
    /**
     * Belows are presented to a user
     */
    var lastTimeDataSync: Long by ReadWriteLong(default = 0)
    var isSyncableWithWifiOnly: Boolean by ReadWriteBoolean(default = false)
    var isAutoSync: Boolean by ReadWriteBoolean(default = true)
    var lastSignedEmail: String by ReadWriteString(default = "")
    var lastSignedName: String by ReadWriteString(default = "")
    var groupName: String by ReadWriteString(default = "abc")

     /**
     * Belows are not presented to a user
     */
    var dbVersion: Int by ReadWriteInt(default = 1)
    var dbNameSuffix: Int by ReadWriteInt(default = 1)
}
