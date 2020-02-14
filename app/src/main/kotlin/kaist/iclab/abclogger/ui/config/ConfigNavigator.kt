package kaist.iclab.abclogger.ui.config

import android.content.Intent
import kaist.iclab.abclogger.ui.base.BaseNavigator

interface ConfigNavigator : BaseNavigator {
    fun navigateIntent(intent: Intent)
    fun navigateBeforeFlush()
    fun navigateBeforeLogout()
    fun navigateAfterLogout()
}