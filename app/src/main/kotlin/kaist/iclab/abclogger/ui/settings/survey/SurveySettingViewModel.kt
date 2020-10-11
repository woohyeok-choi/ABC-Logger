package kaist.iclab.abclogger.ui.settings.survey

import android.app.Application
import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import kaist.iclab.abclogger.structure.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.ui.base.BaseViewModel
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.HttpRequestError
import kaist.iclab.abclogger.structure.survey.SurveyConfiguration
import kaist.iclab.abclogger.commons.getHttp
import kaist.iclab.abclogger.ui.State
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class SurveySettingViewModel(
    private val collector: SurveyCollector,
    savedStateHandle: SavedStateHandle,
    application: Application
) : BaseViewModel(savedStateHandle, application) {
    var baseScheduledDate: Long
        get() = collector.baseScheduleDate
        set(value) {
            collector.baseScheduleDate = value
        }

    suspend fun getConfigurations() = withContext(ioContext) {
        collector.configurations
    }

    suspend fun setConfigurations(configurations: List<SurveyConfiguration>) =
        withContext(ioContext) {
            collector.configurations = configurations
        }

    fun download(url: String?) = flow {
        emit(State.Loading)
        try {
            if (url.isNullOrBlank() || !URLUtil.isValidUrl(url)) throw HttpRequestError.invalidUrl()
            val response = getHttp(url) ?: throw HttpRequestError.emptyContent()
            val survey = Survey.fromJson(response) ?: throw HttpRequestError.invalidJsonFormat()

            emit(State.Success(survey))
        } catch (e: Exception) {
            emit(State.Failure(AbcError.wrap(e)))
        }
    }
}