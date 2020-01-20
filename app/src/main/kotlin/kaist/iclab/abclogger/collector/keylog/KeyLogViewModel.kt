package kaist.iclab.abclogger.collector.keylog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kaist.iclab.abclogger.CollectorPrefs
import kaist.iclab.abclogger.R

class KeyLogViewModel(private val keyLogCollector: KeyLogCollector) : ViewModel() {
    val viewId : MutableLiveData<Int> = MutableLiveData(keyTypeToViewId(CollectorPrefs.softKeyboardType))
    val isAvailable : MutableLiveData<Boolean> = MutableLiveData(keyLogCollector.checkAvailability())

    private fun keyTypeToViewId(keyType: String) = when(keyType) {
        KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN -> R.id.radio_btn_chunjiin
        KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR -> R.id.radio_btn_qwerty
        KeyLogCollector.KEYBOARD_TYPE_UNKNOWN -> R.id.radio_btn_others
        else -> -1
    }

    fun update() {
        isAvailable.postValue(keyLogCollector.checkAvailability())
    }

    fun keyType() : String = when(viewId.value) {
        R.id.radio_btn_chunjiin -> KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN
        R.id.radio_btn_qwerty -> KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR
        else -> KeyLogCollector.KEYBOARD_TYPE_UNKNOWN
    }

}