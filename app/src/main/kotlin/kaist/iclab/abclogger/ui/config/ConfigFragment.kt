package kaist.iclab.abclogger.ui.config


import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.core.Log
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.ui.base.BaseViewModelFragment
import kaist.iclab.abclogger.structure.config.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

abstract class ConfigFragment : BaseViewModelFragment<FragmentConfigBinding, ConfigViewModel>(), ConfigDataAdapter.OnItemClickListener{
    private val configAdapter = ConfigDataAdapter()

    abstract val config: Flow<Config>

    override fun getViewBinding(inflater: LayoutInflater): FragmentConfigBinding =
        FragmentConfigBinding.inflate(inflater)

    override fun initView(viewBinding: FragmentConfigBinding) {
        configAdapter.setOnItemClickListener(this)
        configAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        viewBinding.recyclerView.setHasFixedSize(true)
        viewBinding.recyclerView.itemAnimator = DefaultItemAnimator()
        viewBinding.recyclerView.adapter = configAdapter
    }

    @Suppress("UNCHECKED_CAST")
    override fun afterViewCreated(viewBinding: FragmentConfigBinding) {
        lifecycleScope.launchWhenCreated {
            config.collectLatest { config ->
                configAdapter.config = config
            }
        }
    }

    override fun onItemClick(position: Int, item: ConfigItem<*>) {
        val fragment = this
        lifecycleScope.launchWhenCreated {
            try {
                when (item) {
                    is ActionableConfigItem<*> -> item.run(parentFragmentManager, fragment)
                    is ActivityResultConfigItem<*, *, *> -> item.run(parentFragmentManager, fragment, requireActivity())
                    is RadioConfigItem<*> -> item.run(parentFragmentManager, fragment)
                    is NumberConfigItem -> item.run(parentFragmentManager, fragment)
                    is NumberRangeConfigItem -> item.run(parentFragmentManager, fragment)
                    is TextConfigItem -> item.run(parentFragmentManager, fragment)
                    is CollectorConfigItem -> {
                        val direction = ConfigGeneralFragmentDirections.actionConfigToConfigCollector(
                            qualifiedName = item.qualifiedName,
                            name = item.name,
                            description = item.description ?: ""
                        )
                        findNavController().navigate(direction)
                    }
                }
            } catch (e: Exception) {
                Log.e(this@ConfigFragment.javaClass, e)
                showSnackBar(
                    view = viewBinding.root,
                    message = AbcError.wrap(e).toSimpleString(requireContext())
                )
            }
        }
    }
}