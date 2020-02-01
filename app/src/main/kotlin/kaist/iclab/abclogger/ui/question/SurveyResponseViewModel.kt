package kaist.iclab.abclogger.ui.question

import androidx.lifecycle.*
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.setStatus
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurveyResponseViewModel(private val collector: SurveyCollector) : ViewModel() {
    private val surveyLiveData = MutableLiveData<Pair<SurveyEntity, Survey>>()

    val loadStatus = MutableLiveData<Status>(Status.init())
    val storeStatus = MutableLiveData<Status>(Status.init())
    val instruction = Transformations.map(surveyLiveData) { (_, survey) -> survey.instruction }
    val available = Transformations.map(surveyLiveData) { (entity, _) -> entity.isAvailable() }

    val data = Transformations.map(surveyLiveData) { (entity, survey) ->
        Triple(survey.questions, entity.isAvailable(), entity.showAltText())
    }

    fun load(entityId: Long) = viewModelScope.launch {
        loadStatus.postValue(Status.loading())
        try {
            val data = withContext(Dispatchers.IO) {
                val entity = ObjBox.boxFor<SurveyEntity>()?.get(entityId)
                        ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson(entity.json)
                        ?: throw InvalidSurveyFormatException()
                entity to survey
            }
            loadStatus.postValue(Status.success())
            surveyLiveData.postValue(data)
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun store(entityId: Long,
              reactionTime: Long,
              responseTime: Long,
              onSuccess: (() -> Unit)? = null) = viewModelScope.launch {
        storeStatus.postValue(Status.loading())
        try {
            withContext(Dispatchers.IO) {
                val questions = data.value?.first
                if (questions?.all { it.isCorrectlyAnswered() } != true) throw SurveyIncorrectlyAnsweredException()

                val entity = ObjBox.boxFor<SurveyEntity>()?.get(entityId)
                        ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson(entity.json)
                        ?: throw InvalidSurveyFormatException()

                survey.questions = questions
                entity.reactionTime = reactionTime
                entity.responseTime = responseTime
                entity.json = survey.toJson()

                ObjBox.put(entity)

                val nResponded = (collector.getStatus() as? SurveyCollector.Status)?.nAnswered ?: 0
                collector.setStatus(SurveyCollector.Status(nAnswered = nResponded + 1))
            }
            storeStatus.postValue(Status.success())
            onSuccess?.invoke()
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
        }
    }
}