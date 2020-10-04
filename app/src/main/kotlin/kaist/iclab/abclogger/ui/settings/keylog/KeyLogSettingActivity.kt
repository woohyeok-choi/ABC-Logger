package kaist.iclab.abclogger.ui.settings.keylog

import android.content.Intent
import android.provider.Settings
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.databinding.LayoutSettingKeyLogBinding
import kaist.iclab.abclogger.base.BaseToolbarActivity
import kaist.iclab.abclogger.base.BaseViewModelFragment
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class KeyLogSettingFragment : BaseViewModelFragment<LayoutSettingKeyLogBinding, KeyLogViewModel>() {
    override val viewModel: KeyLogViewModel by viewModel()
    override val innerLayoutId: Int = R.layout.layout_setting_key_log
    override val titleRes: Int = R.string.data_name_key_log
    override val menuId: Int = R.menu.menu_activity_settings
    override val viewModelVariable: Int = BR.viewModel

    override fun beforeExecutePendingBindings() {
        innerViewBinding.containerKeyboardType.setOnClickListener {
            SingleChoiceDialogFragment.showDialog(
                    fragmentManager = supportFragmentManager,
                    title = getString(R.string.setting_key_log_collector_keyboard_type_dialog_title),
                    items = arrayOf(
                            getString(R.string.setting_key_log_collector_chunjiin),
                            getString(R.string.setting_key_log_collector_qwerty),
                            getString(R.string.setting_key_log_collector_others)
                    ),
                    selectedItem = innerViewBinding.txtKeySetting.text?.toString() ?: ""
            ) { content ->
                innerViewBinding.txtKeySetting.text = content
            }
        }

        innerViewBinding.containerAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_activity_settings_save -> {
                lifecycleScope.launch {
                    viewModel.store()
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onError(throwable: Throwable?) {
        showToast(throwable)
    }
}