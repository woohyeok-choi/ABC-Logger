package kaist.iclab.abclogger.ui.survey.list

import androidx.lifecycle.*
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PositionalDataSource
import io.objectbox.query.Query
import io.objectbox.reactive.DataObserver
import kaist.iclab.abclogger.EmptySurveyException
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.SurveyEntity
import kaist.iclab.abclogger.SurveyEntity_
import kaist.iclab.abclogger.ui.Status
import kotlinx.coroutines.*

class SurveyListViewModel : ViewModel() {
    private val factory = SurveyEntityDataSource.Factory(
            query = ObjBox.boxFor<SurveyEntity>().query().orderDesc(SurveyEntity_.deliveredTime).build(),
            scope = viewModelScope
    )
    val entities = LivePagedListBuilder(factory, 20).build()

    val status = Transformations.switchMap(entities) { (it.dataSource as? SurveyEntityDataSource)?.status }

    fun refresh() = entities.value?.dataSource?.invalidate()
}