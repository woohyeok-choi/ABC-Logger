package kaist.iclab.abclogger.ui.settings.polar

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.ui.settings.AbstractSettingActivity
import kaist.iclab.abclogger.commons.showSnackBar
import kaist.iclab.abclogger.databinding.LayoutSettingPolarH10Binding
import kaist.iclab.abclogger.dialog.VersatileDialog
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.stateViewModel

class PolarH10SettingActivity : AbstractSettingActivity<LayoutSettingPolarH10Binding, PolarH10ViewModel>() {
    override val viewModel: PolarH10ViewModel by stateViewModel()

    override fun getInnerViewBinding(inflater: LayoutInflater): LayoutSettingPolarH10Binding =
            LayoutSettingPolarH10Binding.inflate(inflater)

    override fun afterToolbarCreated() {
        childBinding.containerDeviceId.setOnClickListener {
            if (childBinding.swiConnect.isChecked) {
                showSnackBar(viewBinding.root, R.string.setting_polar_h10_msg_require_stop_connection)
            } else {
                lifecycleScope.launchWhenCreated {
                    val newId = VersatileDialog.text(
                        manager = supportFragmentManager,
                        owner = this@PolarH10SettingActivity,
                        title = getString(R.string.collector_polar_h10_info_device_id),
                        value = viewModel.deviceId
                    )?.trim()

                    if (newId != null) {
                        viewModel.deviceId = newId
                        childBinding.txtDeviceIdText.text = newId
                    }
                }
            }
        }

        childBinding.swiConnect.setOnCheckedChangeListener { _, isChecked ->
            childBinding.txtDeviceIdTitle.isEnabled = !isChecked
            childBinding.txtDeviceIdText.isEnabled = !isChecked

            if (isChecked) {
                viewModel.connect(childBinding.txtDeviceIdText.text.toString())
            } else {
                childBinding.txtConnection.text = "DISCONNECTED"
                viewModel.disconnect()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.status.collectLatest { connection ->
                childBinding.txtName.text = connection?.name
                childBinding.txtAddress.text = connection?.address
                childBinding.txtConnection.text = connection?.state
                childBinding.txtRssi.text = connection?.rssi?.toString()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.battery.collectLatest { battery ->
                childBinding.txtBattery.text = battery?.toString()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.heartRate.collectLatest { heartRate ->
                childBinding.txtHeartRate.text = heartRate?.let {
                    "${it.heartRate} (${if (it.contact) "CONTACTED" else "DETACHED"})"
                }
                childBinding.txtRrInterval.text = heartRate?.rrInterval?.lastOrNull()?.toString()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.ecg.collectLatest { ecg ->
                childBinding.txtEcg.text = ecg?.toString()
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.accelerometer.collectLatest { acc ->
                childBinding.txtAccel.text = acc?.let { (x, y, z) -> "$x, $y, $z" }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.error.collectLatest { error ->
                showSnackBar(viewBinding.root, AbcError.wrap(error).toSimpleString(this@PolarH10SettingActivity))
            }
        }

        lifecycleScope.launchWhenCreated {
            val deviceId = viewModel.deviceId
            updateUi(deviceId)

            viewModel.saveState(KEY_DEVICE_ID, deviceId)
        }
    }

    override fun onPause() {
        childBinding.swiConnect.isChecked = false
        super.onPause()
    }

    override fun undo() {
        val deviceId = viewModel.loadState(KEY_DEVICE_ID) ?: ""
        viewModel.deviceId = deviceId
        updateUi(deviceId)
    }

    private fun updateUi(deviceId: String) {
        childBinding.txtDeviceIdText.text = deviceId
    }

    companion object {
        private const val KEY_DEVICE_ID = "KEY_DEVICE_ID"
    }
}