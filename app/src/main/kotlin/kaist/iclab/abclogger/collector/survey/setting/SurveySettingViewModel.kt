package kaist.iclab.abclogger.collector.survey.setting

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.InvalidSurveyFormatException
import kaist.iclab.abclogger.InvalidUrlException
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.setStatus
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.httpGet
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class SurveySettingViewModel(val collector: SurveyCollector) : ViewModel() {
    val loadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val storeStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    /**
     * items are used only in a way of programs.
     */
    val items: MutableLiveData<ArrayList<SurveyCollector.Status.Setting>> = MutableLiveData()

    init {
        viewModelScope.launch {
            loadStatus.postValue(Status.loading())
            try {
                val settings: ArrayList<SurveyCollector.Status.Setting> = arrayListOf<SurveyCollector.Status.Setting>().apply {
                    val savedSettings = (collector.getStatus() as? SurveyCollector.Status)?.settings
                    if (savedSettings.isNullOrEmpty()) {
                        add(SurveyCollector.Status.Setting(id = 1, uuid = UUID.randomUUID().toString()))
                    } else {
                        addAll(savedSettings)
                    }
                }
                items.postValue(settings)
                loadStatus.postValue(Status.success())
            } catch (e: Exception) {
                loadStatus.postValue(Status.failure(e))
            }
        }
    }

    fun removeItem(setting: SurveyCollector.Status.Setting) = viewModelScope.launch {
        if (items.value?.size == 1) return@launch

        items.value?.apply { remove(setting) }?.let { settings -> items.postValue(settings) }
    }

    fun addItem() = viewModelScope.launch {
        items.value?.apply {
            add(SurveyCollector.Status.Setting(id = size + 1, uuid = UUID.randomUUID().toString()))
        }?.let { entities ->
            items.postValue(entities)
        }
    }

    private suspend fun download(url: String?): String = withContext(Dispatchers.IO) {
        if (url == null || !URLUtil.isValidUrl(url)) throw InvalidUrlException()
        val json = httpGet(url) ?: throw InvalidSurveyFormatException()
        Survey.fromJson(json) ?: throw InvalidSurveyFormatException()
        return@withContext json
    }

    fun save(onSuccess: (() -> Unit)? = null) = viewModelScope.launch {
        storeStatus.postValue(Status.loading())
        try {
            items.value?.map { item ->
                item.copy(
                        url = item.url,
                        json = download(item.url),
                        nextTimeTriggered = null,
                        lastTimeTriggered = null
                )
            }?.let { settings ->
                collector.setStatus(SurveyCollector.Status(settings = settings))
            }
            storeStatus.postValue(Status.success())
            onSuccess?.invoke()
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
        }
    }
}