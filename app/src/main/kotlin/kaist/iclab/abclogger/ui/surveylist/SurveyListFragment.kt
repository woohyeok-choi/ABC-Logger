package kaist.iclab.abclogger.ui.surveylist

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.ui.base.BaseFragment
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import kaist.iclab.abclogger.databinding.FragmentSurveyListBinding
import kaist.iclab.abclogger.databinding.SurveyListItemBinding
import kaist.iclab.abclogger.ui.question.SurveyResponseActivity
import kaist.iclab.abclogger.ui.sharedViewNameForDeliveredTime
import kaist.iclab.abclogger.ui.sharedViewNameForMessage
import kaist.iclab.abclogger.ui.sharedViewNameForTitle
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.core.util.Pair as UtilPair

class SurveyListFragment : BaseFragment(){
    private val viewModel : SurveyListViewModel by viewModel()
    private lateinit var binding: FragmentSurveyListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_survey_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        val adapter = SurveyListAdapter().apply {
            onItemClick = { item: SurveyEntity?, binding: SurveyListItemBinding ->
                item?.id?.let { entityId ->
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            UtilPair.create(binding.txtHeader, sharedViewNameForTitle(entityId)),
                            UtilPair.create(binding.txtMessage, sharedViewNameForMessage(entityId)),
                            UtilPair.create(binding.txtDeliveredTime, sharedViewNameForDeliveredTime(entityId))
                    ).toBundle()
                    val intent = Intent(context, SurveyResponseActivity::class.java).fillExtras(
                            SurveyResponseActivity.EXTRA_ENTITY_ID to entityId,
                            SurveyResponseActivity.EXTRA_SHOW_FROM_LIST to true,
                            SurveyResponseActivity.EXTRA_SURVEY_TITLE to item.title,
                            SurveyResponseActivity.EXTRA_SURVEY_MESSAGE to item.message,
                            SurveyResponseActivity.EXTRA_SURVEY_DELIVERED_TIME to item.deliveredTime
                    )
                    startActivity(intent, options)
                }
            }
        }

        binding.recyclerView.adapter = adapter

        binding.swipeLayout.setOnRefreshListener { viewModel.refresh() }

        viewModel.entities.observe(this) { data -> if (data != null) adapter.submitList(data) }
    }
}