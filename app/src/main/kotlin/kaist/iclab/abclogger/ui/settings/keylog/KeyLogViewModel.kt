package kaist.iclab.abclogger.ui.settings.keylog

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.core.ui.BaseViewModel
import kaist.iclab.abclogger.commons.CollectorError
import kaist.iclab.abclogger.core.collector.Status
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

class KeyLogViewModel(
    private val collector: KeyLogCollector,
    savedStateHandle: SavedStateHandle,
    application: Application
) : BaseViewModel(savedStateHandle, application) {
    var keyboardType
        get() = collector.keyboardType
        set(value) {
            collector.keyboardType = value
        }

    fun isAccessibilityServiceAllowed() = collector.isAccessibilityServiceRunning()

}