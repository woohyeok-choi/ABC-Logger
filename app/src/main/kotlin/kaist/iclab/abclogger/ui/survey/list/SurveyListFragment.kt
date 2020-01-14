package kaist.iclab.abclogger.ui.survey.list

import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseFragment
import kaist.iclab.abclogger.databinding.FragmentSurveyListBinding
import kaist.iclab.abclogger.databinding.SurveyListItemBinding
import kaist.iclab.abclogger.ui.survey.question.SurveyResponseActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

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

        val recyclerViewAdapter = SurveyListAdapter().also { adapter ->
            adapter.onItemClick = { item: SurveyEntity?, binding: SurveyListItemBinding ->
                item?.id?.let { entityId ->
                    val bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            requireActivity(),
                            Pair.create(binding.txtHeader, ViewCompat.getTransitionName(binding.txtHeader)),
                            Pair.create(binding.txtMessage, ViewCompat.getTransitionName(binding.txtMessage)),
                            Pair.create(binding.txtDeliveredTime, ViewCompat.getTransitionName(binding.txtDeliveredTime))
                    ).toBundle()

                    startActivity<SurveyResponseActivity>(
                            SurveyResponseActivity.EXTRA_SHOW_FROM_LIST to true,
                            SurveyResponseActivity.EXTRA_SURVEY_ENTITY_ID to entityId,
                            options = bundle
                    )
                }
            }
        }

        binding.recyclerView.apply {
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            itemAnimator = DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }

        binding.swipeLayout.setOnRefreshListener { viewModel.refresh() }

        viewModel.entities.observe(this) { data ->
            if (data != null) recyclerViewAdapter.submitList(data)
        }
    }

    companion object {
        fun newInstance() = SurveyListFragment()
    }
}