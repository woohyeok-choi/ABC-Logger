package kaist.iclab.abclogger.collector.externalsensor.polar.setting

import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingPolarH10Binding
import kaist.iclab.abclogger.ui.dialog.EditTextDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class PolarH10SettingActivity : BaseSettingActivity<LayoutSettingPolarH10Binding, PolarH10ViewModel>() {
    override val viewModel: PolarH10ViewModel by viewModel()

    override val contentLayoutRes: Int = R.layout.layout_setting_polar_h10

    override val titleStringRes: Int = R.string.data_name_polar_h10

    override fun onDestroy() {
        super.onDestroy()
        viewModel.disconnect()
    }

    override fun initialize() {
        dataBinding.viewModel = viewModel

        dataBinding.containerDeviceId.setOnClickListener {
            if(dataBinding.switchOnOff.isChecked) return@setOnClickListener

            EditTextDialogFragment.showDialog(
                    fragmentManager = supportFragmentManager,
                    title = getString(R.string.setting_polar_h10_collector_device_id_dialog_title),
                    content = dataBinding.txtDeviceId.text?.toString() ?: ""
            ) { content -> viewModel.update(content) }
        }

        dataBinding.switchOnOff.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.connect() else (viewModel.disconnect())
        }
    }

    override fun onSaveSelected() {
        viewModel.save()
        finish()
    }
}