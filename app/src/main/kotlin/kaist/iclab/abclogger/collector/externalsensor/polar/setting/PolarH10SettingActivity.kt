package kaist.iclab.abclogger.collector.externalsensor.polar.setting

import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.BR
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.ui.base.BaseToolbarActivity
import kaist.iclab.abclogger.databinding.LayoutSettingPolarH10Binding
import kaist.iclab.abclogger.commons.showToast
import kaist.iclab.abclogger.ui.dialog.EditTextDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PolarH10SettingActivity : BaseToolbarActivity<LayoutSettingPolarH10Binding, PolarH10ViewModel>(), PolarH10Navigator {
    override val viewModel: PolarH10ViewModel by viewModel { parametersOf(this) }

    override val layoutRes: Int = R.layout.layout_setting_polar_h10

    override val titleRes: Int = R.string.data_name_polar_h10

    override val menuId: Int = R.menu.menu_activity_settings

    override val viewModelVariable: Int = BR.viewModel

    override fun beforeExecutePendingBindings() {
        dataBinding.containerDeviceId.setOnClickListener {
            if (dataBinding.switchOnOff.isChecked) return@setOnClickListener

            EditTextDialogFragment.showDialog(
                    fragmentManager = supportFragmentManager,
                    title = getString(R.string.setting_polar_h10_collector_device_id_dialog_title),
                    content = viewModel.deviceId.value?.toString() ?: ""
            ) { content ->
                dataBinding.txtDeviceId.text = content
            }
        }

        dataBinding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) viewModel.connect() else viewModel.disconnect()
        }
    }

    override fun navigateError(throwable: Throwable) {
        showToast(throwable)
    }

    override fun navigateStore() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_activity_settings_save -> {
                viewModel.store()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}