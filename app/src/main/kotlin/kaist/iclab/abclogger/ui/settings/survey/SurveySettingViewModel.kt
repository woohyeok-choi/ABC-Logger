package kaist.iclab.abclogger.collector.survey.config

import android.os.Bundle
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.commons.InvalidSurveyFormatException
import kaist.iclab.abclogger.commons.InvalidUrlException
import kaist.iclab.abclogger.commons.httpGet
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SurveySettingViewModel(val collector: SurveyCollector,
                             navigator: SurveySettingNavigator) : BaseViewModel<SurveySettingNavigator>(navigator) {
    val items: MutableLiveData<ArrayList<SurveyCollector.Status.Setting>> = MutableLiveData()

    override suspend fun onLoad(extras: Bundle?) {
        val settings: ArrayList<SurveyCollector.Status.Setting> = arrayListOf<SurveyCollector.Status.Setting>().apply {
            val savedSettings = collector.getStatus()?.settings
            if (savedSettings.isNullOrEmpty()) {
                add(SurveyCollector.Status.Setting(id = 1, uuid = UUID.randomUUID().toString()))
            } else {
                addAll(savedSettings)
            }
        }
        items.postValue(settings)
    }

    override suspend fun onStore() {
        items.value?.map { item ->
            item.copy(
                    url = item.url,
                    json = withContext(Dispatchers.IO) { download(item.url) },
                    nextTimeTriggered = null,
                    lastTimeTriggered = null
            )
        }?.let { settings ->
            collector.setStatus(
                    SurveyCollector.Status(
                            nReceived = 0,
                            nAnswered = 0,
                            startTime = 0,
                            settings = settings
                    )
            )
        }
        ui { nav?.navigateStore() }
    }

    fun removeItem(setting: SurveyCollector.Status.Setting) {
        if (items.value?.size == 1) return

        items.value?.apply { remove(setting) }?.let { settings -> items.postValue(settings) }
    }

    fun addItem() {
        items.value?.apply {
            add(SurveyCollector.Status.Setting(id = size + 1, uuid = UUID.randomUUID().toString()))
        }?.let { entities ->
            items.postValue(entities)
        }
    }

    private suspend fun download(url: String?): String {
        if (url == null || !URLUtil.isValidUrl(url)) throw InvalidUrlException()
        val json = httpGet(url) ?: throw InvalidSurveyFormatException()
        Survey.fromJson(json) ?: throw InvalidSurveyFormatException()
        return json
    }
}