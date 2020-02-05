package kaist.iclab.abclogger.collector.keylog.setting

import android.content.Intent
import android.provider.Settings
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingKeyLogBinding
import kaist.iclab.abclogger.ui.dialog.SingleChoiceDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class KeyLogSettingActivity : BaseSettingActivity<LayoutSettingKeyLogBinding, KeyLogViewModel>() {
    override val viewModel: KeyLogViewModel by viewModel()

    override val contentLayoutRes: Int
        get() = R.layout.layout_setting_key_log

    override val titleStringRes: Int
        get() = R.string.data_name_key_log

    override fun onResume() {
        super.onResume()
        viewModel.update()
    }

    override fun onSaveSelected() {
        viewModel.save()
        finish()
    }

    override fun initialize() {
        dataBinding.viewModel = viewModel
        dataBinding.containerKeyboardType.setOnClickListener {
            SingleChoiceDialogFragment.showDialog(
                    fragmentManager = supportFragmentManager,
                    title = getString(R.string.setting_key_log_collector_keyboard_type_dialog_title),
                    items = arrayOf(
                            getString(R.string.setting_key_log_collector_chunjiin),
                            getString(R.string.setting_key_log_collector_qwerty),
                            getString(R.string.setting_key_log_collector_others)
                    ),
                    selectedItem = dataBinding.txtKeySetting.text?.toString() ?: ""
            ) { content -> viewModel.update(content) }
        }

        dataBinding.containerAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }
}