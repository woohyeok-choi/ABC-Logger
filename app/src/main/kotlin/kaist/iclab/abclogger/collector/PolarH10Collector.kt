package kaist.iclab.abclogger.collector

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.EditText
import polar.com.sdk.api.PolarBleApiCallback
import polar.com.sdk.api.PolarBleApiDefaultImpl
import polar.com.sdk.api.errors.PolarInvalidArgument
import polar.com.sdk.api.model.PolarDeviceInfo
import polar.com.sdk.api.model.PolarHrData

class PolarH10Collector(val context: Context) :
        BaseCollector{

    companion object {
        private val SHARED_PREF_DEVICE_KEY =
                "kaist.iclab.abclogger.collector.polarh10.PolarH10Collector.DEVICE_KEY"

        class SharedPreferenceUtil(context: Context) {
            private val sharedPref: SharedPreferences =
                    context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)

            fun getValue(key: String): String? = sharedPref.getString(key, null)

            fun putValue(key: String, value: String) {sharedPref.edit().putString(key, value).commit() }

            fun removeKey(key: String) {sharedPref.edit().remove(key).commit()}

            companion object {
                private var PRIVATE_MODE = 0
                private val PREF_NAME = "kaist.iclab.abclogger.SharedPreferenceUtil"

            }
        }
    }

    private val sharedPrefUtil = SharedPreferenceUtil(context)

    private fun getDeviceId(): String? = sharedPrefUtil.getValue(SHARED_PREF_DEVICE_KEY)

    private fun storeDeviceId(deviceId: String) { sharedPrefUtil.putValue(SHARED_PREF_DEVICE_KEY, deviceId)}

    private fun removeDeviceId(){sharedPrefUtil.removeKey(SHARED_PREF_DEVICE_KEY)}

    fun log(type: String, timestamp: Long, msg: String){
        Log.d(type, "timestamp: $timestamp, msg: $msg") }

    private val api = PolarBleApiDefaultImpl.defaultImplementation(context, 15).apply {
        setPolarFilter(false)
        setAutomaticReconnection(true)

        setApiCallback(object : PolarBleApiCallback() {

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "CONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                log("POLAR BATTERY", System.currentTimeMillis(), "level: $level ")
            }

            override fun hrNotificationReceived(identifier: String, data: PolarHrData) {
                log("POLAR HR", System.currentTimeMillis(), "value: ${data.hr}, rrsMs: ${data.rrsMs}, rr: ${data.rrs}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                log("POLAR CONNECTION", System.currentTimeMillis(), "DISCONNECTED: ${polarDeviceInfo.deviceId}")
            }

            override fun ecgFeatureReady(identifier: String) {
                requestEcgSettings(identifier).toFlowable()
                        .flatMap {
                            startEcgStreaming(identifier, it.maxSettings())
                        }
                        .subscribe(
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "time (nanasecond): ${it.timeStamp}, values(yV): ${it.samples}")
                                },
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "Error MSG: ${it.toString()}")
                                },
                                {
                                    log("POLAR ECG", System.currentTimeMillis(), "complete")
                                }
                        )
            }
        })
    }

    override fun start() {
        try {
            api.connectToDevice(getDeviceId()!!)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            removeDeviceId()
            polarInvalidArgument.printStackTrace()
        }
    }

    /**
     * After calling this method - stop(), device ID is no longer stored.
     */
    override fun stop() {
        try {
            api.disconnectFromDevice(getDeviceId()!!)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            polarInvalidArgument.printStackTrace()
        }
        removeDeviceId()
    }

    /**
     * Assuming that permissions are already obtained before calling this method - checkAvailability()
     */
    override fun checkAvailability(): Boolean = (!getDeviceId().isNullOrBlank())

    override fun getRequiredPermissions(): List<String> = listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun newIntentForSetup(): Intent? {
        val deviceIdEditText = EditText(context)
        //deviceIdEditText.setText("4373B624")
        AlertDialog.Builder(context)
                .setTitle("Please type Polar H10 ID")
                .setView(deviceIdEditText)
                .setCancelable(false)
                .setPositiveButton("Done") { dialog, id -> storeDeviceId(deviceIdEditText.text.toString()) }
                .create().show()
        return null

    }
}