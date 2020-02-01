package kaist.iclab.abclogger.ui.surveylist

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.reactivex.rxjava3.schedulers.Schedulers
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity_
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch


class SurveyListViewModel(private val context: Context, private val collector: SurveyCollector) : ViewModel() {
    val responseRates : MutableLiveData<String> = MutableLiveData()

    private suspend fun getResponseRates() =
        (collector.getStatus() as? SurveyCollector.Status)?.let { status ->
            val nReceived = status.nReceived ?: 0
            val nAnswered = status.nAnswered ?: 0
            val rates = if (nReceived > 0) nReceived.toFloat() / nAnswered.toFloat() else 0.0F

            "${context.getString(R.string.general_n_answered)}: $nAnswered" +
                    "/ ${context.getString(R.string.general_n_received)}: $nReceived " +
                    String.format("(%.1f%%)", rates)
        }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            responseRates.postValue(getResponseRates() ?: "")
        }
    }


    private val factory = SurveyEntityDataSource.Factory(
            query = ObjBox.boxFor<SurveyEntity>()?.query()?.orderDesc(SurveyEntity_.deliveredTime)?.build()
    )

    private val config = PagedList.Config.Builder()
            .setPageSize(60)
            .setInitialLoadSizeHint(20)
            .setEnablePlaceholders(true)
            .build()

    val entities = LivePagedListBuilder(factory, config)
            .setFetchExecutor(Dispatchers.IO.asExecutor()).build()

    val status = Transformations.switchMap(factory.source) { source -> source.status }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        entities.value?.dataSource?.invalidate()
        responseRates.postValue(getResponseRates() ?: "")
    }
}