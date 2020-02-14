package kaist.iclab.abclogger.collector.keylog.setting

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseToolbarActivity
import kaist.iclab.abclogger.databinding.LayoutSettingKeyLogBinding
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.ui.dialog.SingleChoiceDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class KeyLogSettingActivity : BaseToolbarActivity<LayoutSettingKeyLogBinding, KeyLogViewModel>(), KeyLogNavigator {
    override val viewModel: KeyLogViewModel by viewModel { parametersOf(this) }

    override val layoutRes: Int = R.layout.layout_setting_key_log

    override val titleRes: Int = R.string.data_name_key_log

    override val menuId: Int = R.menu.menu_activity_settings

    override val viewModelVariable: Int = BR.viewModel

    override fun beforeExecutePendingBindings() {
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
            ) { content ->
                dataBinding.txtKeySetting.text = content
            }
        }

        dataBinding.containerAccessibility.setOnClickListener {
            startActivityForResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_ACCESSIBILITY_SETTING)
        }
    }

    override fun navigateStore() {
        lifecycleScope.launch(Dispatchers.Main) { finish() }
    }

    override fun navigateError(throwable: Throwable) {
        lifecycleScope.launch(Dispatchers.Main) { showToast(throwable) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ACCESSIBILITY_SETTING) viewModel.load()
    }

    companion object {
        const val REQUEST_CODE_ACCESSIBILITY_SETTING = 0xf1
    }
}