package kaist.iclab.abclogger.foreground.adapter

import androidx.lifecycle.*
import androidx.paging.*
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DiffUtil
import android.view.View
import android.view.ViewGroup
import io.objectbox.query.Query
import io.objectbox.reactive.DataObserver
import kaist.iclab.abclogger.App
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.EmptyEntityException
import kaist.iclab.abclogger.common.util.FormatUtils
import kaist.iclab.abclogger.data.entities.ParticipationEntity
import kaist.iclab.abclogger.data.entities.SurveyEntity
import kaist.iclab.abclogger.data.entities.SurveyEntity_
import kaist.iclab.abclogger.foreground.fragment.SurveyListFragment
import kaist.iclab.abclogger.foreground.listener.BaseViewHolder
import kaist.iclab.abclogger.foreground.listener.OnRecyclerViewItemClickListener
import kaist.iclab.abclogger.foreground.view.SurveyItemView
import kaist.iclab.abclogger.survey.SurveyTimeoutPolicyType
import java.util.concurrent.Executors

class SurveyListAdapter : PagedListAdapter<SurveyEntity, SurveyListAdapter.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SurveyEntity>() {
            override fun areItemsTheSame(oldItem: SurveyEntity, newItem: SurveyEntity) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: SurveyEntity, newItem: SurveyEntity) = oldItem == newItem
        }

        fun getEntityViewModel(fragment: androidx.fragment.app.Fragment, showOnlyUnread: Boolean) : EntityViewModel {
            return ViewModelProviders.of(fragment, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                    return EntityViewModel(showOnlyUnread) as? T ?: throw IllegalArgumentException()
                }
            }).get(EntityViewModel::class.java)
        }
    }

    private var listener: OnRecyclerViewItemClickListener<SurveyEntity>? = null


    fun setOnRecyclerViewItemClickListener(onItemClickListener: OnRecyclerViewItemClickListener<SurveyEntity>?) {
        listener = onItemClickListener
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id ?: -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SurveyListAdapter.ViewHolder {
        return ViewHolder(SurveyItemView(parent.context)) { position, view ->
            listener?.onItemClick(position, getItem(position), view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entity = getItem(position)
        if(entity == null) {
            holder.clearView()
        } else {
            holder.bindView(entity)

            ViewCompat.setTransitionName(holder.view.getTitleView(), "${SurveyListFragment.PREFIX_TITLE_VIEW}_${entity.id}")
            ViewCompat.setTransitionName(holder.view.getMessageView(), "${SurveyListFragment.PREFIX_MESSAGE_VIEW}_${entity.id}")
            ViewCompat.setTransitionName(holder.view.getDeliveredTimeView(), "${SurveyListFragment.PREFIX_DELIVERED_TIME_VIEW}_${entity.id}")
        }
    }

    class ViewHolder(surveyItemView: SurveyItemView, onClick: (position: Int, view: View) -> Unit) : BaseViewHolder<SurveyItemView, SurveyEntity>(surveyItemView, onClick) {
        override fun bindView(data: SurveyEntity) {
            view.setTitle(data.title)
            view.setMessage(data.message)
            view.setDeliveredTime(String.format("%s %s",
                FormatUtils.formatSameDay(view.context, data.deliveredTime, System.currentTimeMillis()),
                FormatUtils.formatTimeBefore(data.deliveredTime, System.currentTimeMillis())?.let {"($it)"} ?: ""))
            view.setValidItem(data.isEnableToResponed(System.currentTimeMillis()))
        }

        override fun clearView() {
            view.clearView()
        }
    }

    class EntityDataSource(private val showOnlyUnread: Boolean) : PositionalDataSource<SurveyEntity>() {
        private val observer = DataObserver<List<SurveyEntity>> { invalidate() }
        private var query: Query<SurveyEntity>? = null

        val loadState = MutableLiveData<LoadState>()
        val initialLoadState = MutableLiveData<LoadState>()

        private fun buildQuery(): Query<SurveyEntity> {
            val entity = ParticipationEntity.getParticipatedExperimentFromLocal()
            val query = App.boxFor<SurveyEntity>().let {
                if (showOnlyUnread) {
                    it.query()
                        .less(SurveyEntity_.timestamp, 0)
                        .and()
                        .equal(SurveyEntity_.subjectEmail, entity.subjectEmail)
                        .and()
                        .equal(SurveyEntity_.experimentUuid, entity.experimentUuid)
                        .greater(SurveyEntity_.deliveredTime, entity.participateTime)
                        .orderDesc(SurveyEntity_.deliveredTime)
                        .build()
                } else {
                    it.query()
                        .equal(SurveyEntity_.subjectEmail, entity.subjectEmail)
                        .and()
                        .equal(SurveyEntity_.experimentUuid, entity.experimentUuid)
                        .greater(SurveyEntity_.deliveredTime, entity.participateTime)
                        .orderDesc(SurveyEntity_.deliveredTime)
                        .build()
                }
            }
            query.subscribe().onlyChanges().weak().observer(observer)
            return query
        }

        override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<SurveyEntity>) {
            loadState.postValue(LoadState.LOADING)
            try {
                query = query ?: buildQuery()
                query?.let {
                    val list = it.find(params.startPosition.toLong(), params.loadSize.toLong())
                    callback.onResult(list)
                }
                loadState.postValue(LoadState.LOADED)
            } catch (e: Exception) {
                loadState.postValue(LoadState.ERROR(e))
            }
        }

        override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<SurveyEntity>) {
            initialLoadState.postValue(LoadState.LOADING)
            loadState.postValue(LoadState.LOADING)
            try {
                query = query ?: buildQuery()

                query?.let {
                    val totalCount = it.count().toInt()
                    if (totalCount == 0) {
                        callback.onResult(listOf(), 0, 0)
                        loadState.postValue(LoadState.ERROR(EmptyEntityException()))
                        return
                    }

                    val position = computeInitialLoadPosition(params, totalCount)
                    val loadSize = computeInitialLoadSize(params, position, totalCount)

                    val list = it.find(position.toLong(), loadSize.toLong())

                    if (list.size == loadSize) {
                        callback.onResult(list, position, totalCount)
                    } else {
                        invalidate()
                    }
                    initialLoadState.postValue(LoadState.LOADED)
                    loadState.postValue(LoadState.LOADED)
                }

            } catch (e: Exception) {
                initialLoadState.postValue(LoadState.ERROR(e))
                loadState.postValue(LoadState.ERROR(e))
            }
        }

        class Factory(private val showOnlyUnread: Boolean) : DataSource.Factory<Int, SurveyEntity> () {
            val sourceLiveData = MutableLiveData<EntityDataSource>()

            override fun create(): DataSource<Int, SurveyEntity> {
                val source = EntityDataSource(showOnlyUnread)
                sourceLiveData.postValue(source)
                return source
            }
        }
    }

    class EntityViewModel(showOnlyUnread: Boolean) : ViewModel() {
        private val factory = EntityDataSource.Factory(showOnlyUnread)

        val pagedList = LivePagedListBuilder(factory,
            PagedList.Config.Builder()
            .setInitialLoadSizeHint(20)
            .setPageSize(10)
            .setPrefetchDistance(10)
            .build())
            .setFetchExecutor(Executors.newCachedThreadPool()).build()

        fun refresh() = factory.sourceLiveData.value?.invalidate()

        val loadState : LiveData<LoadState> = Transformations.switchMap(factory.sourceLiveData) { it.loadState }
        val initialLoadState : LiveData<LoadState> = Transformations.switchMap(factory.sourceLiveData) { it.initialLoadState }
    }
}