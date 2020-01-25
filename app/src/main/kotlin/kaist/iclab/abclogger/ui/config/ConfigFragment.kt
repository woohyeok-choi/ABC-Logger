package kaist.iclab.abclogger.ui.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.observe
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.base.BaseFragment
import kaist.iclab.abclogger.databinding.FragmentConfigBinding
import kaist.iclab.abclogger.ui.Status
import kaist.iclab.abclogger.ui.dialog.YesNoDialogFragment
import kaist.iclab.abclogger.ui.splash.SplashActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment() {
    private val viewModel: ConfigViewModel by viewModel()
    private lateinit var dataBinding: FragmentConfigBinding

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
                val handler: (BaseCollector, Throwable?) -> Unit = { c, t ->
                    if (t != null) this@ConfigFragment.showToast(t, false)
                    updateItemView(c)
                }

                if (isChecked) {
                    viewModel.requestStart(collector) { c, t -> handler.invoke(c, t) }
                } else {
                    viewModel.requestStop(collector) { c, t -> handler.invoke(c, t) }
                }
            }
        }

        dataBinding.recyclerView.adapter = adapter
        dataBinding.recyclerView.itemAnimator = null

        dataBinding.configPermission.onClick = {
            startActivityForResult(
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    data = Uri.parse("package:${requireContext().packageName}"),
                    requestCode = REQUEST_CODE_SETTING
            )
        }

        dataBinding.configNetwork.onClick = { _, checked -> viewModel.setUploadForNonMeteredNetwork(checked) }

        dataBinding.configFlushData.onClick = {
            YesNoDialogFragment.showDialog(
                    requireFragmentManager(),
                    getString(R.string.dialog_title_flush_data),
                    getString(R.string.dialog_message_flush_data)
            ) { isYes -> if (isYes) viewModel.flush() }
        }

        dataBinding.configLogout.onClick = {
            YesNoDialogFragment.showDialog(
                    requireFragmentManager(),
                    getString(R.string.dialog_title_sign_out),
                    getString(R.string.dialog_message_sign_out)
            ) { isYes ->
                if (isYes) {
                    viewModel.signOut { isSuccessful ->
                        if (isSuccessful) startActivity<SplashActivity>(
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )
                    }
                }
            }
        }

        dataBinding.configSync.onClick = { viewModel.sync() }

        viewModel.collectors.observe(this) { collectors -> adapter.items = collectors }

        viewModel.flushStatus.observe(this) { status ->
            val ntf = when (status.state) {
                Status.STATE_LOADING -> Notifications.build(
                        context = requireContext(),
                        channelId = Notifications.CHANNEL_ID_PROGRESS,
                        title = getString(R.string.ntf_title_flush),
                        text = getString(R.string.ntf_text_flush),
                        progress = 0,
                        indeterminate = true
                )
                Status.STATE_SUCCESS -> Notifications.build(
                        context = requireContext(),
                        channelId = Notifications.CHANNEL_ID_PROGRESS,
                        title = getString(R.string.ntf_title_flush),
                        text = getString(R.string.ntf_text_flush_complete)
                )
                Status.STATE_FAILURE -> Notifications.build(
                        context = requireContext(),
                        channelId = Notifications.CHANNEL_ID_PROGRESS,
                        title = getString(R.string.ntf_title_flush),
                        text = listOfNotNull(
                                getString(R.string.ntf_text_flush_failed),
                                status.error?.toString(requireContext())
                        ).joinToString(": ")
                )
                else -> null
            }
            if (ntf != null) NotificationManagerCompat.from(requireContext()).notify(Notifications.ID_FLUSH_PROGRESS, ntf)
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.updatePermission()
        viewModel.updateCollectors()
    }

    companion object {
        private const val REQUEST_CODE_SETTING = 0x0f
    }
}