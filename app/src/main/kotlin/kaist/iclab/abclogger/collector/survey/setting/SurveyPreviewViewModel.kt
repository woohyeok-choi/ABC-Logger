package kaist.iclab.abclogger.collector.survey.setting

import android.os.Bundle
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.commons.InvalidSurveyFormatException
import kaist.iclab.abclogger.commons.InvalidUrlException
import kaist.iclab.abclogger.commons.httpGet
import kaist.iclab.abclogger.ui.base.BaseViewModel

class SurveyPreviewViewModel(navigator: SurveyPreviewNavigator) : BaseViewModel<SurveyPreviewNavigator>(navigator) {
    private val surveyData = MutableLiveData<Survey>()

    val title = Transformations.map(surveyData) { survey -> survey?.title ?: "" }
    val message = Transformations.map(surveyData) { survey -> survey?.message ?: "" }
    val instruction = Transformations.map(surveyData) { survey -> survey?.instruction ?: "" }
    val questions = Transformations.map(surveyData) { survey -> survey?.questions ?: arrayOf() }

    override suspend fun onLoad(extras: Bundle?) {
        val url = extras?.getString(SurveyPreviewDialogFragment.ARG_SURVEY_URL) ?: ""
        if (!URLUtil.isValidUrl(url)) throw InvalidUrlException()
        val json = httpGet(url) ?: throw InvalidSurveyFormatException()
        val survey = Survey.fromJson(json) ?: throw InvalidSurveyFormatException()

        surveyData.postValue(survey)
    }

    override suspend fun onStore() {}
}