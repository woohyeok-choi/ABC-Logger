package kaist.iclab.abclogger.ui.surveylist

import android.content.Intent
import android.util.Log
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.observe
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.commons.fillExtras
import kaist.iclab.abclogger.ui.base.BaseFragment
import kaist.iclab.abclogger.databinding.FragmentSurveyListBinding
import kaist.iclab.abclogger.ui.question.SurveyResponseActivity
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.util.Pair as UtilPair

class SurveyListFragment : BaseFragment<FragmentSurveyListBinding, SurveyListViewModel>(){
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

            val intent = Intent(context, SurveyResponseActivity::class.java).fillExtras(
                    SurveyResponseActivity.EXTRA_ENTITY_ID to id,
                    SurveyResponseActivity.EXTRA_SHOW_FROM_LIST to true,
                    SurveyResponseActivity.EXTRA_SURVEY_TITLE to item.title,
                    SurveyResponseActivity.EXTRA_SURVEY_MESSAGE to item.message,
                    SurveyResponseActivity.EXTRA_SURVEY_DELIVERED_TIME to item.deliveredTime
            )
            startActivity(intent, options)
        }

        dataBinding.recyclerView.adapter = adapter
        dataBinding.recyclerView.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        dataBinding.recyclerView.itemAnimator = DefaultItemAnimator()

        dataBinding.swipeLayout.setOnRefreshListener { viewModel.refresh() }

        viewModel.isRefreshing.observe(this) { isRefreshing -> dataBinding.swipeLayout.isRefreshing = isRefreshing }
        viewModel.surveys.observe(this) { data ->
            data.let { adapter.submitList(data) }
        }
    }
}