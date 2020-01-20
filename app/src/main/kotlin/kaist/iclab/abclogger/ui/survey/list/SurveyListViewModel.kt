package kaist.iclab.abclogger.ui.survey.list

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.collector.survey.SurveyEntity_


class SurveyListViewModel : ViewModel() {
    private val factory = SurveyEntityDataSource.Factory(
            query = ObjBox.boxFor<SurveyEntity>().query().orderDesc(SurveyEntity_.deliveredTime).build(),
            scope = viewModelScope
    )
    val entities = LivePagedListBuilder(factory, 20).build()

    val status = Transformations.switchMap(entities) { (it.dataSource as? SurveyEntityDataSource)?.status }

    fun refresh() = entities.value?.dataSource?.invalidate()
}