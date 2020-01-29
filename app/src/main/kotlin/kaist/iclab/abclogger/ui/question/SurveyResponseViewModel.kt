package kaist.iclab.abclogger.ui.question

import androidx.lifecycle.*
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurveyResponseViewModel : ViewModel() {
    private val surveyLiveData = MutableLiveData<Pair<SurveyEntity, Survey>>()

    val loadStatus = MutableLiveData<Status>(Status.init())
    val storeStatus = MutableLiveData<Status>(Status.init())

    val instruction = Transformations.map(surveyLiveData) { (_, survey) -> survey.instruction }
    val availableForProgram = Transformations.map(surveyLiveData) { (entity, _) -> entity.isAvailable() }
    val availableForXml = Transformations.map(surveyLiveData) { (entity, _) -> entity.isAvailable() }
    val showAltText= Transformations.map(surveyLiveData) { (entity, _) -> entity.showAltText() }
    val questions = Transformations.map(surveyLiveData) { (_, survey) -> survey.questions }

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
            surveyLiveData.postValue(data)
            loadStatus.postValue(Status.success())
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
                if (questions.value?.all { it.isCorrectlyAnswered() } != true) throw SurveyIncorrectlyAnsweredException()

                val entity = ObjBox.boxFor<SurveyEntity>()?.get(entityId)
                        ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson(entity.json)
                        ?: throw InvalidSurveyFormatException()

                survey.questions = questions.value ?: survey.questions
                entity.reactionTime = reactionTime
                entity.responseTime = responseTime
                entity.json = survey.toJson()

                ObjBox.put(entity)
            }
            storeStatus.postValue(Status.success())
            onSuccess?.invoke()
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
        }
    }
}