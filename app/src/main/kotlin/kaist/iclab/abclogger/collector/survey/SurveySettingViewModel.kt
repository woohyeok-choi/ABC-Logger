package kaist.iclab.abclogger.collector.survey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.InvalidSurveyFormatException
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
    val settingItems : MutableLiveData<ArrayList<SurveySettingItem>> = MutableLiveData()

    fun initLoad() = viewModelScope.launch {
        loadStatus.postValue(Status.loading())
        try {
            ObjBox.boxFor<SurveySettingEntity>().all.map { entity ->
                SurveySettingItem(entity.url)
            }.let { items ->
                if (items.isEmpty()) {
                    arrayListOf(SurveySettingItem(""))
                } else {
                    arrayListOf<SurveySettingItem>().apply { addAll(items) }
                }
            }.let { items ->
                settingItems.postValue(items)
                loadStatus.postValue(Status.success())
            }
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun removeItem(item: SurveySettingItem) = viewModelScope.launch {
        if(settingItems.value.isNullOrEmpty()) return@launch
        settingItems.value?.apply { remove(item) }?.let { items -> settingItems.postValue(items) }
    }

    fun addItem() = viewModelScope.launch {
        settingItems.value?.apply { add(SurveySettingItem("")) }?.let { items -> settingItems.postValue(items) }
    }

    fun store() = viewModelScope.launch {
        storeStatus.postValue(Status.loading())
        try {
            settingItems.value?.map { item ->
                async {
                    val json = httpGet(item.url) ?: throw InvalidSurveyFormatException()

                    Survey.fromJson(json) ?: throw InvalidSurveyFormatException()

                    SurveySettingEntity(
                            uuid = UUID.randomUUID().toString(),
                            url = item.url,
                            json = json
                    )
                }
            }?.awaitAll()?.let { entities ->
                val box = ObjBox.boxFor<SurveySettingEntity>()
                box.removeAll()
                box.put(entities)
            }
            storeStatus.postValue(Status.success())
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
        }
    }
}