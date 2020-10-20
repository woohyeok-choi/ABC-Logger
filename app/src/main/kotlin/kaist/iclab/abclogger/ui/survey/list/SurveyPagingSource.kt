package kaist.iclab.abclogger.ui.survey.list

import androidx.paging.PagingSource
import io.objectbox.query.QueryBuilder
import kaist.iclab.abclogger.collector.survey.InternalSurveyEntity
import kaist.iclab.abclogger.commons.EntityError
import kaist.iclab.abclogger.core.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * TODO: WHETHER PAGING SOURCE WORKS WELL and RETURN NULLS for END/START OF DATA
 */
class SurveyPagingSource(
    private val dataRepository: DataRepository,
    private val query: QueryBuilder<InternalSurveyEntity>.() -> Unit
) : PagingSource<Long, InternalSurveyEntity>() {
    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, InternalSurveyEntity> =
        withContext(Dispatchers.IO) {
            try {
                if (dataRepository.count(query) == 0L) {
                    throw EntityError.emptyData()
                }

                val limit = params.loadSize.toLong()

                val start = if (params is LoadParams.Refresh) 0 else params.key

                val data = if (start != null) {
                    dataRepository.find(start.coerceAtLeast(0), limit, query)
                } else {
                    listOf()
                }
                val size = data.size

                val prevKey = start?.let {
                    if (it <= 0) null else (it - limit).coerceAtLeast(0)
                }
                val nextKey = if (size == 0) null else (start ?: 0) + size

                LoadResult.Page(
                        data = data,
                        prevKey = prevKey,
                        nextKey = nextKey
                )
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
}