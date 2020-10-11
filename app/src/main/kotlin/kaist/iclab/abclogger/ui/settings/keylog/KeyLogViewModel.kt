package kaist.iclab.abclogger.ui.settings.keylog

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.ui.base.BaseViewModel

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