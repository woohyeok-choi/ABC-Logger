package kaist.iclab.abclogger.collector.keylog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.DataPrefs
import kaist.iclab.abclogger.R
import kotlinx.coroutines.launch

class KeyLogViewModel(private val keyLogCollector: KeyLogCollector) : ViewModel() {
    val viewId : MutableLiveData<Int> = MutableLiveData(when(DataPrefs.statusKeyLog?.keyboardType) {
        KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN -> R.id.radio_btn_chunjiin
        KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR -> R.id.radio_btn_qwerty
        KeyLogCollector.KEYBOARD_TYPE_UNKNOWN -> R.id.radio_btn_others
        else -> -1
    })

    val isAvailable : MutableLiveData<Boolean> = MutableLiveData(keyLogCollector.checkAvailability())

    private val keyType = Transformations.map(viewId) { id ->
        when(id) {
            R.id.radio_btn_chunjiin -> KeyLogCollector.KEYBOARD_TYPE_CHUNJIIN
            R.id.radio_btn_qwerty -> KeyLogCollector.KEYBOARD_TYPE_QWERTY_KOR
            R.id.radio_btn_others -> KeyLogCollector.KEYBOARD_TYPE_UNKNOWN
            else -> ""
        }
    }

    fun update() {
        isAvailable.postValue(keyLogCollector.checkAvailability())
    }

    fun save() = viewModelScope.launch {
        DataPrefs.statusKeyLog = DataPrefs.statusKeyLog?.copy(keyboardType = keyType.value ?: "")
    }

}