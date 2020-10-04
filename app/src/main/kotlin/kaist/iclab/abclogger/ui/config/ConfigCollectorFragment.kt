package kaist.iclab.abclogger.ui.config.list

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import kaist.iclab.abclogger.BuildConfig
import kaist.iclab.abclogger.base.BaseViewModelFragment
import kaist.iclab.abclogger.databinding.FragmentConfigCollectorBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import kaist.iclab.abclogger.commons.crossFade
import kaist.iclab.abclogger.commons.getFragmentResult
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.ui.State
import kaist.iclab.abclogger.ui.config.*
import kaist.iclab.abclogger.ui.config.dialog.*
import kotlinx.coroutines.flow.collectLatest

class ConfigCollectorFragment : BaseViewModelFragment<FragmentConfigCollectorBinding, ConfigViewModel>(), ConfigDataAdapter.OnItemClickListener {
    override val viewModel: ConfigViewModel by sharedViewModel()

    private val args: ConfigCollectorFragmentArgs by navArgs()
    private val configAdapter = ConfigDataAdapter()
    private val shortAnimDuration by lazy { resources.getInteger(android.R.integer.config_shortAnimTime).toLong() }

    override fun getViewBinding(inflater: LayoutInflater): FragmentConfigCollectorBinding =
            FragmentConfigCollectorBinding.inflate(inflater)

    override fun afterViewCreated(context: Context) {
        configAdapter.setOnItemClickListener(this)

        viewBinding.recyclerView.adapter = configAdapter
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBinding.recyclerView.visibility = View.GONE

        viewBinding.txtError.visibility = View.GONE

        viewBinding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launchWhenResumed {
            viewModel.getCollectorConfig(args.qualifiedName).collectLatest { state ->
                when (state) {
                    is State.Loading -> {
                        viewBinding.progressBar.visibility = View.VISIBLE
                        viewBinding.recyclerView.visibility = View.GONE
                        viewBinding.txtError.visibility = View.GONE
                    }
                    is State.Failure -> {
                        showSnackBar(viewBinding.root, state.error)

                        crossFade(
                                fadeIn = viewBinding.txtError,
                                fadeOut = viewBinding.progressBar,
                                duration = shortAnimDuration
                        )
                    }
                    is State.Success<*> -> {
                        (state.data as? ArrayList<*>)?.filterIsInstance<ConfigData>()?.let {
                            configAdapter.items = arrayListOf<ConfigData>().apply { addAll(it) }
                        }

                        crossFade(
                                fadeIn = viewBinding.recyclerView,
                                fadeOut = viewBinding.progressBar,
                                duration = shortAnimDuration
                        )
                    }
                }
            }
        }
    }

    override fun onItemClick(position: Int, item: ConfigItem<*>) {
        lifecycleScope.launchWhenCreated {
            when (item) {
                is ActionableConfigItem<*> -> onItemClick(item)
                is ActivityResultConfigItem<*, *, *> -> onItemClick(item)
                is BooleanConfigItem -> onItemClick(item)
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
            ConfirmDialogFragment.newInstance(REQUEST_KEY, item.name, message).show(parentFragmentManager, null)
            val result = getFragmentResult(REQUEST_KEY)
            if (ConfirmDialogFragment.isNegative(result)) return
        }
        item.trigger()
    }

    private suspend fun onItemClick(item: ActivityResultConfigItem<*, *, *>) {
        item.trigger(this)
    }

    private suspend fun onItemClick(item: BooleanConfigItem) {
        val direction = ConfigCollectorFragmentDirections.actionConfigCollectorToConfigDialogBoolean(
                requestKey = REQUEST_KEY,
                title = item.name,
                isChecked = item.value ?: false,
                textChecked = item.format.invoke(true),
                textUnchecked = item.format.invoke(false)
        )
        findNavController().navigate(direction)
        val result = getFragmentResult(REQUEST_KEY)
        item.value = BottomDialogBooleanFragment.getResult(result)
    }

    private suspend fun onItemClick(item: RadioConfigItem<*>) {
        val direction = ConfigCollectorFragmentDirections.actionConfigCollectorToConfigDialogRadio(
                requestKey = REQUEST_KEY,
                title = item.name,
                options = item.optionsAsString(),
                value = item.getIndex()
        )
        findNavController().navigate(direction)
        val result = getFragmentResult(REQUEST_KEY)
        item.setIndex(BottomDialogRadioFragment.getResult(result))
    }

    private suspend fun onItemClick(item: NumberConfigItem) {
        val direction = ConfigCollectorFragmentDirections.actionConfigCollectorToConfigDialogNumber(
                requestKey = REQUEST_KEY,
                title = item.name,
                options = item.optionsAsString(),
                value = item.value ?: item.min
        )
        findNavController().navigate(direction)
        val result = getFragmentResult(REQUEST_KEY)
        item.value = BottomDialogNumberFragment.getResult(result)
    }

    private suspend fun onItemClick(item: NumberRangeConfigItem) {
        val direction = ConfigCollectorFragmentDirections.actionConfigCollectorToConfigDialogRange(
                requestKey = REQUEST_KEY,
                title = item.name,
                options = item.optionsAsString(),
                fromValue = item.value?.first ?: item.min,
                toValue = item.value?.second ?: item.max
        )
        findNavController().navigate(direction)
        val result = getFragmentResult(REQUEST_KEY)
        item.value = BottomDialogRangeFragment.getResult(result)
    }

    private suspend fun onItemClick(item: TextConfigItem) {
        val direction = ConfigCollectorFragmentDirections.actionConfigCollectorToConfigDialogText(
                requestKey = REQUEST_KEY,
                title = item.name,
                inputType = item.inputType ?: InputType.TYPE_CLASS_TEXT,
                hint = item.hint,
                value = item.value
        )
        findNavController().navigate(direction)
        val result = getFragmentResult(REQUEST_KEY)
        item.value = BottomDialogTextFragment.getResult(result)
    }

    companion object {
        private const val REQUEST_KEY = "${BuildConfig.APPLICATION_ID}.ui.config.list.ConfigCollectorFragment.REQUEST_KEY"
    }


}