package kaist.iclab.abclogger.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class LoadViewModel<R> : ViewModel() {
    data class Status<R>(val state: Int, val result: R? = null, val error: Exception? = null)

    val statusLiveData = MutableLiveData<Status<R>>(Status(STATE_INIT))

    abstract fun loadData(): R

    fun load() = viewModelScope.launch(Dispatchers.IO) {
        statusLiveData.postValue(Status(state = STATE_LOADING))
        try {
            val result = loadData()
            statusLiveData.postValue(Status(state = STATE_SUCCESS, result = result))
        } catch (e: Exception) {
            statusLiveData.postValue(Status(state = STATE_FAILURE, error = e))
        }
    }

    companion object {
        const val STATE_INIT = 0
        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1
    }
}