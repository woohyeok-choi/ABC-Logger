package kaist.iclab.abclogger.ui.survey

import android.content.Intent
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.fillExtras
import kaist.iclab.abclogger.databinding.FragmentSurveyListBinding
import kaist.iclab.abclogger.base.BaseViewModelFragment
import kaist.iclab.abclogger.ui.question.SurveyResponseFragment
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.util.Pair as UtilPair

class SurveyListFragment : BaseViewModelFragment<FragmentSurveyListBinding, SurveyListViewModel>() {
    override val layoutId: Int = R.layout.fragment_survey_list

    override val viewModelVariable: Int = BR.viewModel

    override val viewModel: SurveyListViewModel by viewModel()

    override fun beforeExecutePendingBindings() {
        val adapter = SurveyListAdapter()
        adapter.setOnItemClick { item, binding ->
            val id = item?.id ?: return@setOnItemClick
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    UtilPair.create(binding.txtHeader, sharedViewNameForTitle(id)),
                    UtilPair.create(binding.txtMessage, sharedViewNameForMessage(id)),
                    UtilPair.create(binding.txtDeliveredTime, sharedViewNameForDeliveredTime(id))
            ).toBundle()

            val intent = Intent(context, SurveyResponseFragment::class.java).fillExtras(
                    SurveyResponseFragment.EXTRA_ENTITY_ID to id,
                    SurveyResponseFragment.EXTRA_SHOW_FROM_LIST to true,
                    SurveyResponseFragment.EXTRA_SURVEY_TITLE to item.title,
                    SurveyResponseFragment.EXTRA_SURVEY_MESSAGE to item.message,
                    SurveyResponseFragment.EXTRA_SURVEY_DELIVERED_TIME to item.deliveredTime
            )
            startActivity(intent, options)
        }

        viewBinding.recyclerView.adapter = adapter
        viewBinding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()

        viewBinding.swipeLayout.setOnRefreshListener { viewModel.refresh() }

        viewModel.isRefreshing.observe(this) { isRefreshing -> viewBinding.swipeLayout.isRefreshing = isRefreshing }
        viewModel.surveys.observe(this) { data ->
            data.let { adapter.submitList(data) }
        }
    }
}