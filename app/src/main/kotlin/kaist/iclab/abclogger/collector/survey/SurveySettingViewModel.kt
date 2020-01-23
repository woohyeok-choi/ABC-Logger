package kaist.iclab.abclogger.collector.survey

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.InvalidSurveyFormatException
import kaist.iclab.abclogger.InvalidUrlException
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.httpGet
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class SurveySettingViewModel : ViewModel() {
    val loadStatus : MutableLiveData<Status> = MutableLiveData(Status.init())
    val storeStatus : MutableLiveData<Status> = MutableLiveData(Status.init())

    /**
     * settingItems are used only in a way of programs.
     */
    val items : MutableLiveData<ArrayList<SurveySettingEntity>> = MutableLiveData()

    private suspend fun download(url: String) : SurveySettingEntity = withContext(Dispatchers.IO) {
        if (!URLUtil.isValidUrl(url)) throw InvalidUrlException()
        val json = httpGet(url) ?: throw InvalidSurveyFormatException()
        Survey.fromJson(json) ?: throw InvalidSurveyFormatException()

        return@withContext SurveySettingEntity(
                url = url,
                json = json
        )
    }

    fun load() = viewModelScope.launch {
        loadStatus.postValue(Status.loading())

        try {
            val entities = withContext(Dispatchers.IO) {
                arrayListOf<SurveySettingEntity>().apply {
                    val box = ObjBox.boxFor<SurveySettingEntity>()
                    if (box == null || box.count() == 0L) {
                        add(SurveySettingEntity(uuid = UUID.randomUUID().toString()))
                    } else {
                        box.all.forEach { entity -> add(entity) }
                    }
                }
            }
            items.postValue(entities)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun removeItem(entity: SurveySettingEntity) = viewModelScope.launch {
        if(items.value?.size == 1) return@launch

        items.value?.apply {
            remove(entity)
        }?.let { entities ->
            items.postValue(entities)
        }
    }

    fun addItem() = viewModelScope.launch {
        items.value?.apply {
            add(SurveySettingEntity(uuid = UUID.randomUUID().toString()))
        }?.let { entities ->
            items.postValue(entities)
        }
    }

    fun store(onComplete: ((isSuccessful: Boolean) -> Unit)? = null) = viewModelScope.launch {
        storeStatus.postValue(Status.loading())

        try {
            items.value?.map { item ->
                download(item.url)
            }?.let { entities ->
                ObjBox.boxFor<SurveySettingEntity>()?.also { box ->
                    box.removeAll()
                    box.put(entities)
                }
            }
            storeStatus.postValue(Status.success())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
            onComplete?.invoke(false)
        }
    }
}