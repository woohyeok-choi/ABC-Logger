package kaist.iclab.abclogger.collector

import android.content.Intent
import androidx.lifecycle.LiveData
import kaist.iclab.abclogger.Base
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.background.Status
import kaist.iclab.abclogger.common.util.PermissionUtils
import kaist.iclab.abclogger.fillBaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface BaseCollector {
    fun start()
    fun stop()
    fun checkAvailability() : Boolean
    fun getRequiredPermissions() : List<String>
    fun newIntentForSetup(): Intent?
}