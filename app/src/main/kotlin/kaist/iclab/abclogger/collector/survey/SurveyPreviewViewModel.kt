package kaist.iclab.abclogger.collector.survey

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.InvalidSurveyFormatException
import kaist.iclab.abclogger.httpGet
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.launch
import java.lang.Exception

class SurveyPreviewViewModel: ViewModel() {
    val loadStatus = MutableLiveData<Status>(Status.init())
    val survey = MutableLiveData<Survey>()

    fun load(url: String) = viewModelScope.launch {
        loadStatus.postValue(Status.loading())
        try {
            val json = httpGet(url) ?: throw InvalidSurveyFormatException()
            val parsedSurvey = Survey.fromJson(json) ?: throw InvalidSurveyFormatException()
            survey.postValue(parsedSurvey)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }
}