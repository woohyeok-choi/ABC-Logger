package kaist.iclab.abclogger.collector.survey

import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
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
    val loadStatus = MutableLiveData<Status>(Status.init())
    val title = MutableLiveData<String>()
    val message = MutableLiveData<String>()
    val instruction = MutableLiveData<String>()

    /**
     *  Here, questions are used only in a way of programs.
     */
    val questions = MutableLiveData<Array<Survey.Question>>()

    fun load(url: String) = viewModelScope.launch {
        loadStatus.postValue(Status.loading())

        try {
            if (!URLUtil.isValidUrl(url)) throw InvalidUrlException()

            val survey = withContext(Dispatchers.IO) {
                val json = httpGet(url) ?: throw InvalidSurveyFormatException()
                Survey.fromJson(json) ?: throw InvalidSurveyFormatException()
            }

            title.postValue(survey.title)
            message.postValue(survey.message)

            instruction.postValue(survey.instruction)
            questions.postValue(survey.questions)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }
}