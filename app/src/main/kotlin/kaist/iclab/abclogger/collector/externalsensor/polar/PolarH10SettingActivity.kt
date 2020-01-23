package kaist.iclab.abclogger.collector.externalsensor.polar

import android.app.Activity
import kaist.iclab.abclogger.CollectorPrefs
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.base.BaseSettingActivity
import kaist.iclab.abclogger.databinding.LayoutSettingPolarH10Binding
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
        dataBinding.btnConnect.setOnClickListener {
            viewModel.connect()
        }
    }

    override fun onSaveSelected() {
        CollectorPrefs.polarH10DeviceId = viewModel.deviceId.value ?: ""
        setResult(Activity.RESULT_OK)
        finish()
    }
}