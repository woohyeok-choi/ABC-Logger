package kaist.iclab.abclogger.ui.survey.list

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import io.objectbox.query.Query
import io.objectbox.reactive.DataObserver
import kaist.iclab.abclogger.EmptySurveyException
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SurveyEntityDataSource(private val query: Query<SurveyEntity>,
                             private val scope: CoroutineScope) : PositionalDataSource<SurveyEntity>() {
    private val observer = DataObserver<List<SurveyEntity>> { invalidate() }

    val status = MutableLiveData<Status>(Status.init())

    init {
        query.subscribe().onlyChanges().weak().observer(observer)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<SurveyEntity>) {
        status.postValue(Status.loading())
        scope.launch(Dispatchers.IO) {
            try {
                query.find(params.startPosition.toLong(), params.loadSize.toLong()).let { data ->
                    callback.onResult(data)
                }
                status.postValue(Status.success())
            } catch (e: Exception) {
                status.postValue(Status.failure(e))
            }
        }
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<SurveyEntity>) {
        status.postValue(Status.loading())

        scope.launch {
            try {
                val count = query.count().toInt()
                if (count == 0) {
                    throw EmptySurveyException()
                }
                val position = computeInitialLoadPosition(params, count)
                val loadSize = computeInitialLoadSize(params, position, count)

                val data = query.find(position.toLong(), loadSize.toLong())

                if (data.size == loadSize) {
                    callback.onResult(data, position, count)
                } else {
                    invalidate()
                }
                status.postValue(Status.success())
            } catch (e: Exception) {
                status.postValue(Status.failure(e))
            }
        }
    }

    override fun invalidate() {
        super.invalidate()
        scope.cancel()
    }

    class Factory(private val query: Query<SurveyEntity>, private val scope: CoroutineScope) : DataSource.Factory<Int, SurveyEntity>() {
        override fun create(): DataSource<Int, SurveyEntity> = SurveyEntityDataSource(query, scope)
    }
}