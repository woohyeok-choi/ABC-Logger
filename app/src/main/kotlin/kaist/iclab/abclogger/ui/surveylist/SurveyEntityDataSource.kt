package kaist.iclab.abclogger.ui.surveylist

import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import io.objectbox.query.Query
import io.objectbox.reactive.DataObserver
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.commons.EmptySurveyException
import kaist.iclab.abclogger.ui.Status

class SurveyEntityDataSource(private val query: Query<SurveyEntity>?) : PositionalDataSource<SurveyEntity>() {
    private val observer = DataObserver<List<SurveyEntity>> { invalidate() }
    val initStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val status: MutableLiveData<Status> = MutableLiveData(Status.init())

    init {
        query?.subscribe()?.onlyChanges()?.weak()?.observer(observer)
    }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<SurveyEntity>) {
        initStatus.postValue(Status.loading())
        try {
            val count = query?.count()?.toInt() ?: 0
            if (count == 0) {
                callback.onResult(listOf(), 0, 0)
                throw EmptySurveyException()
            }
            val position = computeInitialLoadPosition(params, count)
            val loadSize = computeInitialLoadSize(params, position, count)

            val data = query?.find(position.toLong(), loadSize.toLong()) ?: listOf()

            if (data.size == loadSize) {
                callback.onResult(data, position, count)
            } else {
                invalidate()
            }
            initStatus.postValue(Status.success())
        } catch (e: Exception) {
            initStatus.postValue(Status.failure(e))
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<SurveyEntity>) {
        status.postValue(Status.loading())
        try {
            val data = query?.find(params.startPosition.toLong(), params.loadSize.toLong())
                    ?: listOf()
            callback.onResult(data)
            status.postValue(Status.success())
        } catch (e: Exception) {
            status.postValue(Status.failure(e))
        }
    }

    class Factory(private val query: Query<SurveyEntity>?) : DataSource.Factory<Int, SurveyEntity>() {
        val sourceData: MutableLiveData<SurveyEntityDataSource> = MutableLiveData()

        override fun create(): DataSource<Int, SurveyEntity> {
            val source = SurveyEntityDataSource(query)
            sourceData.postValue(source)
            return source
        }
    }
}