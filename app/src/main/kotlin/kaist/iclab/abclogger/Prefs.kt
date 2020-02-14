package kaist.iclab.abclogger

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.*
import kaist.iclab.abclogger.BuildConfig


object Prefs : PreferenceHolder(name = BuildConfig.PREF_NAME) {
    /**
     * Belows are presented to a user
     */
    var lastTimeDataSync: Long by ReadWriteLong(default = 0)
    var canUploadMeteredNetwork: Boolean by ReadWriteBoolean(default = false)
    var maxDbSize : Long by ReadWriteLong(default = 0)
    var isAutoSync: Boolean by ReadWriteBoolean(default = false)

    /**
     * Belows are not presented to a user
     */
    var dbVersion : Int by ReadWriteInt(default = 0)
}
