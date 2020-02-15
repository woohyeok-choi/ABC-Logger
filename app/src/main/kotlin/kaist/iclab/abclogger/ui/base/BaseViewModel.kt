package kaist.iclab.abclogger.ui.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.*

import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

abstract class BaseViewModel<N : BaseNavigator>(navigator: N? = null) : ViewModel() {
    private val refNavigator: WeakReference<N?> = WeakReference(navigator)

    val loadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val storeStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val nav: N? = refNavigator.get()

    fun launch(call: suspend () -> Unit) = viewModelScope.launch { call() }

    suspend fun io(call: suspend () -> Unit) = withContext(Dispatchers.IO) { call() }

    suspend fun ui(call: suspend () -> Unit) = withContext(Dispatchers.Main) { call() }

    fun load(extras: Bundle? = null) = launch {
        loadStatus.postValue(Status.loading())
        try {
            io { onLoad(extras) }
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun store() = launch {
        storeStatus.postValue(Status.loading())
        try {
            io { onStore() }
            storeStatus.postValue(Status.success())
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
            storeStatus.postValue(Status.failure(e))
        }
    }

    @CallSuper
    override fun onCleared() {
        refNavigator.clear()
        super.onCleared()
    }

    abstract suspend fun onLoad(extras: Bundle? = null)

    abstract suspend fun onStore()
}