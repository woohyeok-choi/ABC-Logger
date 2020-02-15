package kaist.iclab.abclogger.ui.question

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.commons.InvalidEntityIdException
import kaist.iclab.abclogger.commons.InvalidSurveyFormatException
import kaist.iclab.abclogger.commons.SurveyIncorrectlyAnsweredException
import kaist.iclab.abclogger.ui.base.BaseViewModel

class SurveyResponseViewModel(private val collector: SurveyCollector,
                              navigator: SurveyResponseNavigator) : BaseViewModel<SurveyResponseNavigator>(navigator) {
    private val surveyInternal: MutableLiveData<Pair<SurveyEntity, Survey>> = MutableLiveData()

    val title: MutableLiveData<String> = MutableLiveData()
    val message: MutableLiveData<String> = MutableLiveData()
    val deliveredTime: MutableLiveData<Long> = MutableLiveData()

    val setting = Transformations.map(surveyInternal) { (entity, survey) ->
        Triple(survey.questions, entity.isAvailable(), entity.showAltText())
    }

    val available = Transformations.map(surveyInternal) { (entity, _) -> entity.isAvailable() }
    val instruction = Transformations.map(surveyInternal) { (_, survey) -> survey.instruction }

    private var reactionTime: Long = 0

    override suspend fun onLoad(extras: Bundle?) {
        reactionTime = System.currentTimeMillis()
        title.postValue(extras?.getString(SurveyResponseActivity.EXTRA_SURVEY_TITLE))
        message.postValue(extras?.getString(SurveyResponseActivity.EXTRA_SURVEY_MESSAGE))
        deliveredTime.postValue(extras?.getLong(SurveyResponseActivity.EXTRA_SURVEY_DELIVERED_TIME, 0))

        val id = extras?.getLong(SurveyResponseActivity.EXTRA_ENTITY_ID, 0) ?: 0
        val entity = ObjBox.get<SurveyEntity>(id) ?: throw InvalidEntityIdException()
        val survey = Survey.fromJson(entity.json) ?: throw InvalidSurveyFormatException()
        surveyInternal.postValue(entity to survey)
    }

    override suspend fun onStore() {
        val responseTime = System.currentTimeMillis()

        val questions = setting.value?.first
        if (questions?.all { question -> question.isCorrectlyAnswered() } != true) throw SurveyIncorrectlyAnsweredException()
        val data = surveyInternal.value ?: throw InvalidEntityIdException()
        val (entity, survey) = data

        survey.questions = questions
        entity.reactionTime = reactionTime
        entity.responseTime = responseTime
        entity.json = survey.toJson()

        ObjBox.put(entity)

        val nResponded = collector.getStatus()?.nAnswered ?: 0
        collector.setStatus(SurveyCollector.Status(nAnswered = nResponded + 1))

        ui { nav?.navigateStore() }
    }
}