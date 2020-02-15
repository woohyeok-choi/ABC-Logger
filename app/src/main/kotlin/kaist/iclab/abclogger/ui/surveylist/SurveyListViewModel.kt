package kaist.iclab.abclogger.ui.surveylist

import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.survey.SurveyCollector
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity_
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class SurveyListViewModel(private val context: Context,
                          private val collector: SurveyCollector) : ViewModel() {
    private val executor = Executors.newSingleThreadExecutor()

    private val factory = SurveyEntityDataSource.Factory(ObjBox.query<SurveyEntity>()?.orderDesc(SurveyEntity_.deliveredTime)?.build())

    val responseRates : MutableLiveData<String> = MutableLiveData()

    val surveys = LivePagedListBuilder(
            factory,
            PagedList.Config.Builder()
                    .setPageSize(60)
                    .setEnablePlaceholders(true)
                    .setMaxSize(200)
                    .build()
    ).setFetchExecutor { runnable -> executor.execute(runnable) }.build()

    val initStatus = Transformations.switchMap(factory.sourceData) { source -> source.initStatus }

    val status = Transformations.switchMap(factory.sourceData) { source -> source.status }

    val isRefreshing = Transformations.map(initStatus) { status -> status?.state == Status.STATE_LOADING }

    init {
        loadResponseRates()
    }

    private fun loadResponseRates() = viewModelScope.launch(Dispatchers.IO) {
        val status = collector.getStatus()
        val nAnswered = status?.nAnswered ?: 0
        val nReceived = status?.nReceived ?: 0
        val rates = if (nReceived > 0) nAnswered.toFloat() / nReceived.toFloat() * 100 else 0.0F

        val msg = context.getString(
                R.string.general_response_rates,
                nAnswered, nReceived, String.format("%.1f%%", rates)
        )
        responseRates.postValue(msg)
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        factory.sourceData.value?.invalidate()
        loadResponseRates()
    }
}