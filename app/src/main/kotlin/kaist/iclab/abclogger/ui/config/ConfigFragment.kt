package kaist.iclab.abclogger.ui.config

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseFragment
import kaist.iclab.abclogger.collector.activity.ActivityCollector
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val viewModel : ConfigViewModel by viewModel()
    private lateinit var dataBinding : FragmentConfigBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_config, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = this

        val adapter = DataConfigListAdapter().apply {
            onClick = { collector ->
                if (viewModel.isAllPermissionGranted.value != true) {
                    this@ConfigFragment.showToast(R.string.msg_permission_setting_required)
                } else {
                    collector.newIntentForSetUp?.let { startActivityForResult(it, REQUEST_CODE_SETTING) }
                }
            }
            onCheckedChanged = { collector, isChecked ->
                if (isChecked) {
                    viewModel.requestStart(collector) { throwable ->
                        this@ConfigFragment.showToast(throwable, false)
                    }
                }  else {
                    viewModel.requestStop(collector) { throwable ->
                        this@ConfigFragment.showToast(throwable, false)
                    }
                }
            }
        }

        dataBinding.recyclerView.adapter = adapter

        setupGeneralListeners(dataBinding)

        view.context.getSharedPreferences(
                BuildConfig.PREF_NAME_COLLECTOR,
                Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(this)

        view.context.getSharedPreferences(
                BuildConfig.PREF_NAME_GENERAL,
                Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(this)

        viewModel.collectors.observe(this) { collectors -> adapter.items = collectors }

        viewModel.flushStatus.observe(this) { (status, isLogout) ->
            /**
             * TODO: Show indeterminate progressbar until completion
             */
        }
    }

    private fun setupGeneralListeners(binding: FragmentConfigBinding) {
        binding.configPermission.onClick = {
            startActivityForResult(
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    data = Uri.parse("package:${requireContext().packageName}"),
                    requestCode = REQUEST_CODE_SETTING
            )
        }

        binding.configNetwork.onClick = {_, checked ->
            viewModel.setUploadForNonMeteredNetwork(checked)
        }

        binding.configSync.onClick = {
            /**
             * TODO: Implementing sync
             */
        }

        binding.configFlushData.onClick = {
            viewModel.flush()
        }

        binding.configLogout.onClick = {
            viewModel.signOut()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.load()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        viewModel.load()
    }

    companion object {
        private const val REQUEST_CODE_SETTING = 0x0f
    }

}