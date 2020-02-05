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
import kaist.iclab.abclogger.ui.base.BaseFragment
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.splash.SplashActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val viewModel: ConfigViewModel by viewModel()
    private lateinit var dataBinding: FragmentConfigBinding

    private fun startPermissionActivity() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${requireContext().packageName}"))
        startActivityForResult(intent, REQUEST_CODE_SETTING)
    }

    private fun startSplashActivity() {
        val intent = Intent(requireContext(), SplashActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private fun registerPreferenceListener(context: Context) {
        context.getSharedPreferences(
                BuildConfig.PREF_NAME, Context.MODE_PRIVATE
        ).registerOnSharedPreferenceChangeListener(this)
    }

    private fun unregisterPreferenceListener(context: Context) {
        context.getSharedPreferences(
                BuildConfig.PREF_NAME, Context.MODE_PRIVATE
        ).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        registerPreferenceListener(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_config, container, false)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.viewModel = viewModel
        dataBinding.lifecycleOwner = this

        val adapter = ConfigListAdapter()

        adapter.onCheckedChanged = { key, item, isChecked ->
            if (item is DataConfigItem) {
                if (isChecked) {
                    viewModel.requestStart(key)
                } else {
                    viewModel.requestStop(key)
                }
            } else {
                when(key) {
                    PrefKeys.CAN_UPLOAD_METERED_NETWORK -> viewModel.setUploadSetting(isChecked)
                }
            }
        }

        adapter.onClick = { key, item ->
            if (item is DataConfigItem) {
                item.intentForSetup?.let { startActivity(it) }
            } else {
                when(key) {
                    PrefKeys.LAST_TIME_SYNC -> viewModel.sync()
                    PrefKeys.PERMISSION -> startPermissionActivity()
                    PrefKeys.MAX_DB_SIZE -> {
                        YesNoDialogFragment.showDialog(
                                parentFragmentManager,
                                getString(R.string.dialog_title_flush_data),
                                getString(R.string.dialog_message_flush_data)
                        ) { viewModel.flush() }
                    }
                    PrefKeys.LOGOUT -> {
                        YesNoDialogFragment.showDialog(
                                parentFragmentManager,
                                getString(R.string.dialog_title_sign_out),
                                getString(R.string.dialog_message_sign_out)
                        ) { viewModel.logout { startSplashActivity() } }
                    }
                }
            }
        }

        dataBinding.recyclerView.adapter = adapter
        dataBinding.recyclerView.itemAnimator = null

        viewModel.configs.observe(this) { configs ->
            if(configs != null) adapter.items = configs
        }

        viewModel.errorStatus.observe(this) { status ->
            if (status.error != null) showToast(status.error)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPreferenceListener(requireContext())
    }

    companion object {
        private const val REQUEST_CODE_SETTING = 0x0f
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        viewModel.update()
    }
}