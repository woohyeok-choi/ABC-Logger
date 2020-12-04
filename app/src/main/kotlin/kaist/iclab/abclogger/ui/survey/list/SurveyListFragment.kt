package kaist.iclab.abclogger.ui.survey.list

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.paging.filter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.databinding.FragmentSurveyListBinding
import kaist.iclab.abclogger.ui.base.BaseViewModelFragment
import kaist.iclab.abclogger.collector.survey.InternalSurveyEntity
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.EntityError
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.ItemSurveyListBinding
import kaist.iclab.abclogger.ui.State
import kaist.iclab.abclogger.ui.survey.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.stateSharedViewModel

@ExperimentalCoroutinesApi
@FlowPreview
abstract class SurveyListFragment :
    BaseViewModelFragment<FragmentSurveyListBinding, SurveyViewModel>(),
    SurveyListAdapter.OnItemClickListener {
    override val viewModel: SurveyViewModel by stateSharedViewModel()
    abstract val listType: Int

    @get:StringRes
    abstract val typeStringRes: Int

    private val listAdapter by lazy {
        SurveyListAdapter(
            header = getString(typeStringRes),
            isEnabled = when (listType) {
                LIST_TYPE_ANSWERED, LIST_TYPE_EXPIRED -> false
                LIST_TYPE_NOT_ANSWERED -> true
                else -> null
            }
        )
    }

    override fun getViewBinding(inflater: LayoutInflater): FragmentSurveyListBinding =
        FragmentSurveyListBinding.inflate(inflater)

    abstract fun getDirection(
        id: Long,
        title: String?,
        message: String?,
        triggerTime: Long?,
        restore: Boolean = false
    ): NavDirections

    override fun initView(viewBinding: FragmentSurveyListBinding) {
        listAdapter.setOnItemClickListener(this)

        viewBinding.recyclerView.adapter = listAdapter.withLoadStateHeaderAndFooter(
            SurveyLoadStateAdapter(),
            SurveyLoadStateAdapter()
        )

        viewBinding.recyclerView.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBinding.recyclerView.visibility = View.GONE
        viewBinding.txtSurveyError.visibility = View.GONE

        viewBinding.swipeLayout.setOnRefreshListener {
            listAdapter.refresh()
        }
    }

    override fun afterViewCreated(viewBinding: FragmentSurveyListBinding) {
        lifecycleScope.launchWhenCreated {
            listAdapter.loadStateFlow.collectLatest { state ->
                val refresh = state.refresh
                val append = state.append
                val prepend = state.prepend

                viewBinding.swipeLayout.isRefreshing = refresh is LoadState.Loading
                viewBinding.recyclerView.visibility =
                    if (refresh is LoadState.Error) View.GONE else View.VISIBLE
                viewBinding.txtSurveyError.visibility =
                    if (refresh is LoadState.Error) View.VISIBLE else View.GONE

                when {
                    refresh is LoadState.Error -> {
                        if (refresh.error is EntityError.EmptyData) {
                            viewBinding.txtSurveyError.text =
                                getString(R.string.survey_msg_no_records, getString(typeStringRes))
                        } else {
                            viewBinding.txtSurveyError.text =
                                getString(R.string.survey_msg_error_occurs)
                            error(refresh.error, R.string.action_refresh) { listAdapter.refresh() }
                        }
                    }

                    append is LoadState.Error ->
                        error(append.error, R.string.action_retry) { listAdapter.retry() }
                    prepend is LoadState.Error ->
                        error(prepend.error, R.string.action_retry) { listAdapter.retry() }
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.saveStateFlow.collectLatest { status ->
                if (status is State.Failure) {
                    val direction = getDirection(
                        id = 0,
                        title = null,
                        message = null,
                        triggerTime = null,
                        restore = true
                    )
                    error(status.error, R.string.action_resolve) {
                        findNavController().navigate(direction)
                    }
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            val entities = when (listType) {
                LIST_TYPE_ANSWERED -> viewModel.listAnswered()
                LIST_TYPE_NOT_ANSWERED -> viewModel.listNotAnswered()
                LIST_TYPE_EXPIRED -> viewModel.listExpired()
                else -> viewModel.listAll()
            }
            entities.collectLatest { data ->
                listAdapter.submitData(
                    data.insertHeaderItem(InternalSurveyEntity().apply {
                        id = -1L
                    })
                )
            }
        }
    }

    override fun onItemClick(
        position: Int,
        binding: ItemSurveyListBinding,
        item: InternalSurveyEntity
    ) {
        val directions = getDirection(
            id = item.id,
            title = binding.title,
            message = binding.message,
            triggerTime = item.actualTriggerTime,
            restore = false                 // means that it does started by "restore"
        )

        val extras = FragmentNavigatorExtras(
            binding.txtTitle to sharedViewNameForTitle(item.id),
            binding.txtMessage to sharedViewNameForMessage(item.id),
            binding.txtTriggeredTime to sharedViewNameForDeliveredTime(item.id)
        )

        findNavController().navigate(directions, extras)
    }

    private fun error(throwable: Throwable?) {
        Log.e(javaClass, throwable)

        showSnackBar(viewBinding.root, AbcError.wrap(throwable).toSimpleString(requireContext()))
    }

    private fun error(throwable: Throwable?, actionRes: Int, action: () -> Unit) {
        Log.e(javaClass, throwable)

        showSnackBar(
            viewBinding.root,
            AbcError.wrap(throwable).toSimpleString(requireContext()),
            false,
            actionRes,
            action
        )
    }

    companion object {
        const val LIST_TYPE_ALL = 0x01
        const val LIST_TYPE_NOT_ANSWERED = 0x02
        const val LIST_TYPE_ANSWERED = 0x03
        const val LIST_TYPE_EXPIRED = 0x04
    }

}