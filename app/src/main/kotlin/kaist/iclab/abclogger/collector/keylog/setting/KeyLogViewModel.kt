package kaist.iclab.abclogger.collector.keylog.setting

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.checkAccessibilityService
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.ui.base.BaseViewModel

class KeyLogViewModel(private val context: Context,
                      private val collector: KeyLogCollector,
                      navigator: KeyLogNavigator) : BaseViewModel<KeyLogNavigator>(navigator) {
    val keyboardName : MutableLiveData<String> = MutableLiveData()
    val available : MutableLiveData<Boolean> = MutableLiveData()

    override suspend fun onLoad(extras: Bundle?) {
        val name = when(collector.getStatus()?.keyboardType) {
            KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN -> context.getString(R.string.setting_key_log_collector_chunjiin)
            KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR -> context.getString(R.string.setting_key_log_collector_qwerty)
            KeyLogCollector.KEYBOARD_TYPE_OTHERS -> context.getString(R.string.setting_key_log_collector_others)
            else -> null
        }
        keyboardName.postValue(name)
        available.postValue(checkAccessibilityService<KeyLogCollector.KeyLogCollectorService>(context))
    }

    override suspend fun onStore() {
        val keyboardType = when(keyboardName.value) {
            context.getString(R.string.setting_key_log_collector_chunjiin) ->
                KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN
            context.getString(R.string.setting_key_log_collector_qwerty) ->
                KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR
            context.getString(R.string.setting_key_log_collector_others) ->
                KeyLogCollector.KEYBOARD_TYPE_OTHERS
            else -> null
        }
        collector.setStatus(KeyLogCollector.Status(keyboardType = keyboardType))
        ui { nav?.navigateStore() }
    }
}