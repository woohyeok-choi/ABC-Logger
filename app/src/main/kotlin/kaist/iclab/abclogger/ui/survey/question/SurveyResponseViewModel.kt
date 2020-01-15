package kaist.iclab.abclogger.ui.survey.question

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SurveyResponseViewModel : ViewModel() {
    data class SurveySetting(val survey: Survey,
                             val deliveredTime: Long,
                             val isAvailable: Boolean,
                             val showEtc: Boolean
    )

    val surveySetting = MutableLiveData<SurveySetting>()
    val loadStatus = MutableLiveData<Status>(Status.init())
    val storeStatus = MutableLiveData<Status>(Status.init())

    fun load(entityId: Long) {
        loadStatus.postValue(Status.loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = ObjBox.boxFor<SurveyEntity>().get(entityId) ?: throw InvalidEntityIdException()
                val survey = Survey.fromJson<Survey>(entity.json) ?: throw InvalidSurveyFormatException()
                surveySetting.postValue(SurveySetting(survey, entity.deliveredTime, entity.isAvailable(), entity.showAltText()))

                loadStatus.postValue(Status.success())
            } catch (e: Exception) {
                loadStatus.postValue(Status.failure(e))
            }
        }
    }

    fun store(entityId: Long, reactionTime: Long, responseTime: Long) {
        storeStatus.postValue(Status.loading())

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = ObjBox.boxFor<SurveyEntity>().get(entityId) ?: throw InvalidEntityIdException()
                entity.reactionTime = reactionTime
                entity.responseTime = responseTime

                val json = surveySetting.value?.survey?.toJson() ?: return@launch
                entity.json = json

                ObjBox.boxFor<SurveyEntity>().put(entity)
                storeStatus.postValue(Status.success())
            } catch (e: Exception) {
                storeStatus.postValue(Status.failure(e))
            }
        }
    }
}