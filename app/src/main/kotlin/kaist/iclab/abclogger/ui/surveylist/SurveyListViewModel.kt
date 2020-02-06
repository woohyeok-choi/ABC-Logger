package kaist.iclab.abclogger.ui.surveylist

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import io.objectbox.android.ObjectBoxDataSource
import io.reactivex.rxjava3.schedulers.Schedulers
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.getStatus
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity_
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class SurveyListViewModel(private val context: Context, private val collector: SurveyCollector) : ViewModel() {
    val responseRates : MutableLiveData<String> = MutableLiveData()

    private suspend fun getResponseRates() =
        (collector.getStatus() as? SurveyCollector.Status)?.let { status ->
            val nAnswered = status.nAnswered ?: 0
            val nReceived = status.nReceived ?: 0
            val rates = if (nReceived > 0) nAnswered.toFloat() / nReceived.toFloat() * 100 else 0.0F

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

    val entities = LivePagedListBuilder(factory, 20)
            .setFetchExecutor(Executors.newSingleThreadExecutor()).build()

    val status = Transformations.switchMap(factory.source) { source -> source.status }

    val isRefreshing = Transformations.map(status) { status -> status?.state == Status.STATE_LOADING }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        entities.value?.dataSource?.invalidate()
        responseRates.postValue(getResponseRates() ?: "")
    }
}