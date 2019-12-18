package kaist.iclab.abclogger.foreground.fragment

import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.common.base.BaseFragment
import kaist.iclab.abclogger.common.type.LoadState
import kaist.iclab.abclogger.common.type.LoadStatus
import kaist.iclab.abclogger.common.ABCException
import kaist.iclab.abclogger.foreground.activity.ExperimentDetailActivity
import kaist.iclab.abclogger.foreground.activity.MainActivity
import kaist.iclab.abclogger.foreground.adapter.ExperimentListAdapter
import kaist.iclab.abclogger.foreground.listener.OnRecyclerViewItemClickListener
// import kaist.iclab.abc.protos.ExperimentProtos
import kotlinx.android.synthetic.main.fragment_experiment_list.*


class ExperimentListFragment : BaseFragment(), OnRecyclerViewItemClickListener<UInt> {
    override fun onItemClick(position: Int, item: UInt?, view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var recyclerViewAdapter: ExperimentListAdapter
    private lateinit var viewModel: ExperimentListAdapter.ExperimentDataViewModel


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_experiment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        setupListener()
        setupObserver()

        bindView()
    }

    private fun setupView() {
        recyclerViewAdapter = ExperimentListAdapter().apply {
            //setOnItemClickListener(this@ExperimentListFragment)
        }
    }

    private fun setupObserver() {
        viewModel = ViewModelProviders.of(this).get(ExperimentListAdapter.ExperimentDataViewModel::class.java)

        viewModel.pagedList.observe(this, Observer {
            recyclerViewAdapter.submitList(it)
        })

        viewModel.initialLoadState.observe(this, Observer {
            swipeLayout.isRefreshing = it == LoadState.LOADING
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            recyclerView.visibility = if(it?.status == LoadStatus.SUCCESS) View.VISIBLE else View.GONE

            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })

        viewModel.loadState.observe(this, Observer {
            swipeLayout.isRefreshing = it == LoadState.LOADING
            txtError.visibility = if (it?.status == LoadStatus.FAILED) View.VISIBLE else View.GONE
            recyclerView.visibility = if (it?.status == LoadStatus.FAILED) View.GONE else View.VISIBLE

            if (it?.error != null) txtError.setText(
                if (it.error is ABCException) it.error.getErrorStringRes() else R.string.error_general_error
            )
        })
    }

    private fun setupListener() {
        swipeLayout.setOnRefreshListener { viewModel.refresh() }
    }

    private fun bindView() {
        recyclerView.apply {
            addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(context, androidx.recyclerview.widget.LinearLayoutManager.VERTICAL))
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            adapter = recyclerViewAdapter
        }
    }

    /*
    override fun onItemClick(position: Int, item: UInt, view: View) {
        /*
        item?.basic?.uuid?.let {
            startActivity(ExperimentDetailActivity.newIntent(requireContext(), item), ViewCompat.getTransitionName(view)?.let { name ->
                ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, name)
            }?.toBundle())
        }
        */
        if (position == 0)
            return Unit
    }
    */
}