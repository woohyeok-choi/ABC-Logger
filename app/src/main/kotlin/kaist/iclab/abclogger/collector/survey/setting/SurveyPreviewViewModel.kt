package kaist.iclab.abclogger.collector.survey

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.InvalidSurveyFormatException
import kaist.iclab.abclogger.InvalidUrlException
import kaist.iclab.abclogger.httpGet
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

class SurveyPreviewViewModel: ViewModel() {
    private val surveyLiveData = MutableLiveData<Survey>()
    val loadStatus = MutableLiveData<Status>(Status.init())

    val title = Transformations.map(surveyLiveData) { survey -> survey?.title ?: "" }
    val message = Transformations.map(surveyLiveData) { survey -> survey?.message ?: "" }
    val instruction = Transformations.map(surveyLiveData) { survey -> survey?.instruction ?: ""}

    /**
     *  Here, questions are used only in a way of programs.
     */
    val questions = Transformations.map(surveyLiveData) { survey -> survey?.questions ?: arrayOf() }

    fun load(url: String) = viewModelScope.launch {
        loadStatus.postValue(Status.loading())

        try {
            if (!URLUtil.isValidUrl(url)) throw InvalidUrlException()

            val survey = withContext(Dispatchers.IO) {
                val json = httpGet(url) ?: throw InvalidSurveyFormatException()
                Survey.fromJson(json) ?: throw InvalidSurveyFormatException()
            }
            surveyLiveData.postValue(survey)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }
}