package kaist.iclab.abclogger.ui.survey.question

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SurveyResponseViewModel : ViewModel() {
    val loadEntity = MutableLiveData<SurveyEntity>()
    val loadSurvey = MutableLiveData<Survey>()
    val loadStatus = MutableLiveData<Status>(Status.init())
    val storeStatus = MutableLiveData<Status>(Status.init())

    fun load(entityId: Long) {
        loadStatus.postValue(Status.loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = ObjBox.boxFor<SurveyEntity>().get(entityId) ?: throw InvalidEntityIdException()
                loadEntity.postValue(entity)

                val survey = Survey.fromJson<Survey>(entity.json) ?: throw InvalidSurveyFormatException()
                loadSurvey.postValue(survey)

                loadStatus.postValue(Status.success())
            } catch (e: Exception) {
                loadStatus.postValue(Status.failure(e))
            }
        }
    }

    fun store(entityId: Long, questions: Array<SurveyQuestion>) {
        storeStatus.postValue(Status.loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = ObjBox.boxFor<SurveyEntity>().get(entityId) ?: throw InvalidEntityIdException()
                val parsedSurvey = Survey.fromJson<Survey>(entity.json) ?: throw InvalidSurveyFormatException()

                parsedSurvey.questions = questions
                entity.json = parsedSurvey.toJson()

                ObjBox.boxFor<SurveyEntity>().put(entity)
                storeStatus.postValue(Status.success())
            } catch (e: Exception) {
                storeStatus.postValue(Status.failure(e))
            }
        }
    }
}