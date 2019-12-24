package kaist.iclab.abclogger.collector

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kaist.iclab.abclogger.DataTrafficEntity
import kaist.iclab.abclogger.SharedPrefs
import kaist.iclab.abclogger.fillBaseInfo

class DataTrafficCollector(val context: Context) : BaseCollector {
    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    private val dataListener = object : PhoneStateListener() {
        val directions = arrayOf(
                TelephonyManager.DATA_ACTIVITY_IN,
                TelephonyManager.DATA_ACTIVITY_IN,
                TelephonyManager.DATA_ACTIVITY_INOUT
        )

        var totalRxBytes: Long = -1
        var totalTxBytes: Long = -1
        var mobileRxBytes: Long = -1
        var mobileTxBytes: Long = -1
        var prevTimestamp: Long = -1


        override fun onDataActivity(direction: Int) {
            super.onDataActivity(direction)
            if (direction in directions) {
                if (prevTimestamp < 0) {
                    prevTimestamp = System.currentTimeMillis()
                    totalRxBytes = TrafficStats.getTotalRxBytes()
                    totalTxBytes = TrafficStats.getTotalTxBytes()
                    mobileRxBytes = TrafficStats.getMobileRxBytes()
                    mobileTxBytes = TrafficStats.getMobileTxBytes()
                } else {
                    val curTimestamp = System.currentTimeMillis()
                    val curTotalRxBytes = TrafficStats.getTotalRxBytes()
                    val curTotalTxBytes = TrafficStats.getTotalTxBytes()
                    val curMobileRxBytes = TrafficStats.getMobileRxBytes()
                    val curMobileTxBytes = TrafficStats.getMobileTxBytes()

                    val netTotalRxBytes = curTotalRxBytes - totalRxBytes
                    val netTotalTxBytes = curTotalTxBytes - totalTxBytes
                    val netMobileRxBytes = curMobileRxBytes - mobileRxBytes
                    val netMobileTxBytes = curMobileTxBytes - mobileTxBytes

                    DataTrafficEntity(
                            fromTime = prevTimestamp,
                            rxBytes = netTotalRxBytes,
                            txBytes = netTotalTxBytes,
                            mobileRxBytes = netMobileRxBytes,
                            mobileTxBytes = netMobileTxBytes
                    ).fillBaseInfo(timestamp = curTimestamp).run {
                        putEntity(this)
                    }

                    prevTimestamp = curTimestamp
                    totalRxBytes = curTotalRxBytes
                    totalTxBytes = curTotalTxBytes
                    mobileRxBytes = curMobileRxBytes
                    mobileTxBytes = curMobileTxBytes
                }
            }
        }
    }

    override fun start() {
        if (!SharedPrefs.isProvidedDataTraffic || !checkAvailability()) return

        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_DATA_ACTIVITY)
    }

    override fun stop() {
        if (!SharedPrefs.isProvidedDataTraffic || !checkAvailability()) return

        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_NONE)
    }

    override fun checkAvailability(): Boolean = true

    override fun getRequiredPermissions(): List<String> = listOf()

    override fun newIntentForSetup(): Intent? = null
}