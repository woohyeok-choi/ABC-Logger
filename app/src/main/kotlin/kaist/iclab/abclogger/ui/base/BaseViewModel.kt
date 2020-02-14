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

    fun load(extras: Bundle? = null) = viewModelScope.launch(Dispatchers.IO) {
        loadStatus.postValue(Status.loading())
        try {
            onLoad(extras)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            nav?.navigateError(e)
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun store() = viewModelScope.launch(Dispatchers.IO) {
        storeStatus.postValue(Status.loading())
        try {
            onStore()
            storeStatus.postValue(Status.success())
        } catch (e: Exception) {
            nav?.navigateError(e)
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