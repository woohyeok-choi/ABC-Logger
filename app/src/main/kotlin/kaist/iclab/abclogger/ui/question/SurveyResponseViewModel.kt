package kaist.iclab.abclogger.ui.question

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.survey.Survey
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurveyResponseViewModel : ViewModel() {
    val instruction = MutableLiveData<String>()
    val loadStatus = MutableLiveData<Status>(Status.init())
    val storeStatus = MutableLiveData<Status>(Status.init())

    val setting = MutableLiveData<Triple<Array<Survey.Question>, Boolean, Boolean>>()

    fun load(entityId: Long) = viewModelScope.launch {
        loadStatus.postValue(Status.loading())
        try {
            val data = withContext(Dispatchers.IO) {
                val entity = ObjBox.boxFor<SurveyEntity>()?.get(entityId)
                        ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson(entity.json)
                        ?: throw InvalidSurveyFormatException()
                instruction.postValue(survey.instruction)
                Triple(survey.questions, entity.isAvailable(), entity.showAltText())
            }
            setting.postValue(data)
            loadStatus.postValue(Status.success())
        } catch (e: Exception) {
            loadStatus.postValue(Status.failure(e))
        }
    }

    fun store(entityId: Long,
              reactionTime: Long,
              responseTime: Long,
              onComplete: ((isSuccessful: Boolean) -> Unit)? = null) = viewModelScope.launch {
        storeStatus.postValue(Status.loading())
        try {
            withContext(Dispatchers.IO) {
                val entity = ObjBox.boxFor<SurveyEntity>()?.get(entityId)
                        ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson(entity.json)
                        ?: throw InvalidSurveyFormatException()
                val questions = setting.value?.first
                survey.questions = questions ?: survey.questions

                entity.reactionTime = reactionTime
                entity.responseTime = responseTime
                entity.json = survey.toJson()

                ObjBox.boxFor<SurveyEntity>()?.put(entity)
            }
            storeStatus.postValue(Status.success())
            onComplete?.invoke(true)
        } catch (e: Exception) {
            storeStatus.postValue(Status.failure(e))
            onComplete?.invoke(false)
        }
    }


}