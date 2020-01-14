package kaist.iclab.abclogger.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class LoadViewModel: ViewModel() {

    val status = MutableLiveData<Status>(Status(STATE_INIT))

    abstract fun onLoad()

    fun load() = viewModelScope.launch {
        status.postValue(Status(state = STATE_LOADING))
        try {
            onLoad()
            status.postValue(Status(state = STATE_SUCCESS))
        } catch (e: Exception) {
            status.postValue(Status(state = STATE_FAILURE, error = e))
        }
    }

    companion object {
        const val STATE_INIT = 0
        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1


    }
}