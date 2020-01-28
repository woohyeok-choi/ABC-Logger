package kaist.iclab.abclogger.collector.keylog.setting

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.checkAccessibilityService
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.keylog.KeyLogCollector
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class KeyLogViewModel(private val context: Context, private val collector: KeyLogCollector) : ViewModel() {
    val keyboardName : MutableLiveData<String> = MutableLiveData()
    val available : MutableLiveData<Boolean> = MutableLiveData()

    init {
        viewModelScope.launch {
            val name = when((collector.getStatus() as? KeyLogCollector.Status)?.keyboardType) {
                KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN -> context.getString(R.string.setting_key_log_collector_chunjiin)
                KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR -> context.getString(R.string.setting_key_log_collector_qwerty)
                KeyLogCollector.KEYBOARD_TYPE_OTHERS -> context.getString(R.string.setting_key_log_collector_others)
                else -> context.getString(R.string.general_unknown)
            }
            keyboardName.postValue(name)
            available.postValue(checkAccessibilityService<KeyLogCollector.KeyLogCollectorService>(context))
        }
    }

    fun update(newKeyboard: String? = null) = viewModelScope.launch {
        newKeyboard?.let { keyboardName.postValue(it) }
        available.postValue(checkAccessibilityService<KeyLogCollector.KeyLogCollectorService>(context))
    }

    fun save() = GlobalScope.launch {
        val keyboardType = when(keyboardName.value) {
            context.getString(R.string.setting_key_log_collector_chunjiin) ->
                KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN
            context.getString(R.string.setting_key_log_collector_qwerty) ->
                KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR
            context.getString(R.string.setting_key_log_collector_others) ->
                KeyLogCollector.KEYBOARD_TYPE_OTHERS
            else -> ""
        }
        collector.setStatus(KeyLogCollector.Status(keyboardType = keyboardType))
    }
}