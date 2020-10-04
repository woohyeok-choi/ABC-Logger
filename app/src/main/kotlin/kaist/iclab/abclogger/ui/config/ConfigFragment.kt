package kaist.iclab.abclogger.ui.config

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.base.BaseViewModelFragment
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.dialog.VersatileDialog
import kaist.iclab.abclogger.ui.State
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

@ExperimentalCoroutinesApi
@FlowPreview
abstract class ConfigFragment2 : BaseViewModelFragment<FragmentConfigBinding, ConfigViewModel>(), ConfigDataAdapter.OnItemClickListener {
    private val configAdapter = ConfigDataAdapter()

    override fun getViewBinding(inflater: LayoutInflater): FragmentConfigBinding =
            FragmentConfigBinding.inflate(inflater)

    override fun initView(viewBinding: FragmentConfigBinding) {
        configAdapter.setOnItemClickListener(this)

        viewBinding.recyclerView.adapter = configAdapter
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBinding.recyclerView.visibility = View.GONE

        viewBinding.txtError.visibility = View.GONE

        viewBinding.swipeLayout.setOnRefreshListener {
            refresh()
        }
    }

    abstract fun refresh()
    abstract val config: Flow<State>

    override fun afterViewCreated(viewBinding: FragmentConfigBinding) {
        lifecycleScope.launchWhenResumed {
            config.collectLatest { state ->
                viewBinding.swipeLayout.isRefreshing = state == State.Loading
                when (state) {
                    is State.Failure -> {
                        showSnackBar(viewBinding.root, state.error)

                        viewBinding.recyclerView.visibility = View.VISIBLE
                        viewBinding.txtError.visibility = View.GONE
                    }
                    is State.Success<*> -> {
                        (state.data as? ArrayList<*>)?.filterIsInstance<ConfigData>()?.let {
                            configAdapter.items = arrayListOf<ConfigData>().apply { addAll(it) }
                        }
                        viewBinding.recyclerView.visibility = View.GONE
                        viewBinding.txtError.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onItemClick(position: Int, item: ConfigItem<*>) {
        lifecycleScope.launchWhenCreated {
            when (item) {
                is ActionableConfigItem<*> -> onItemClick(item)
                is CollectorConfigItem -> onItemClick(item)
                is ActivityResultConfigItem<*, *, *> -> onItemClick(item)
                is RadioConfigItem<*> -> onItemClick(item)
                is NumberConfigItem -> onItemClick(item)
                is NumberRangeConfigItem -> onItemClick(item)
                is TextConfigItem -> onItemClick(item)
            }
        }
    }

    private suspend fun onItemClick(item: ActionableConfigItem<*>) {
        val message = item.confirmDialogMessage

        if (!message.isNullOrBlank()) {
            val result = VersatileDialog.confirm(
                    manager = parentFragmentManager,
                    owner = this,
                    title = item.name,
                    message = message
            )
            if (result) item.run()
        } else {
            item.run()
        }
    }

    private suspend fun onItemClick(item: CollectorConfigItem) {
        val direction = ConfigFragmentDirections.actionConfigToConfigCollector(
                qualifiedName = item.qualifiedName
        )
        findNavController().navigate(direction)
    }

    private suspend fun onItemClick(item: ActivityResultConfigItem<*, *, *>) {
        item.run(this)
    }


    private suspend fun onItemClick(item: RadioConfigItem<*>) {
        val result = VersatileDialog.singleChoice(
                manager = parentFragmentManager,
                owner = this,
                title = item.name,
                value = item.index,
                items = item.optionsAsString()
        )
        if (result != null) item.index = result
    }

    private suspend fun onItemClick(item: NumberConfigItem) {
        val result = VersatileDialog.slider(
                manager = parentFragmentManager,
                owner = this,
                title = item.name,
                value = item.value,
                from = item.min,
                to = item.max,
                step = item.step,
                nFloats = item.nFloats
        )
        if (result != null) item.value = result
    }

    private suspend fun onItemClick(item: NumberRangeConfigItem) {
        val result = VersatileDialog.sliderRange(
                manager = parentFragmentManager,
                owner = this,
                title = item.name,
                value = item.value,
                from = item.min,
                to = item.max,
                step = item.step,
                nFloats = item.nFloats
        )
        if (result != null) item.value = result
    }

    private suspend fun onItemClick(item: TextConfigItem) {
        val result = VersatileDialog.text(
                manager = parentFragmentManager,
                owner = this,
                title = item.name,
                value = item.value,
                inputType = item.inputType
        )
        if (result != null) item.value = result
    }

    companion object {
        private const val REQUEST_KEY = "${BuildConfig.APPLICATION_ID}.ui.config.list.ConfigFragment.REQUEST_KEY"
    }


}