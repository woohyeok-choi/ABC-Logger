package kaist.iclab.abclogger.background.collector

import android.content.Intent
import androidx.lifecycle.LiveData
import kaist.iclab.abclogger.background.Status

interface BaseCollector {
    fun start()
    fun stop()
    fun checkAvailability() : Boolean
    fun getRequiredPermissions() : List<String>
    fun newIntentForSetup(): Intent?
    fun getLiveStatus() : LiveData<Status>
}