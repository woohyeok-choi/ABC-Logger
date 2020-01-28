package kaist.iclab.abclogger.collector.traffic

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.collector.setStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class DataTrafficCollector(val context: Context) : BaseCollector {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): String = ""
    }

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    private val timestamp : AtomicLong = AtomicLong(0)
    private val totalRxBytes : AtomicLong = AtomicLong(0)
    private val totalTxBytes : AtomicLong = AtomicLong(0)
    private val mobileRxBytes : AtomicLong = AtomicLong(0)
    private val mobileTxBytes : AtomicLong = AtomicLong(0)

    private val directions = arrayOf(
            TelephonyManager.DATA_ACTIVITY_IN,
            TelephonyManager.DATA_ACTIVITY_OUT,
            TelephonyManager.DATA_ACTIVITY_INOUT
    )

    private suspend fun handleDataActivity() {
        val curTime = System.currentTimeMillis()
        val curTotalRxBytes = TrafficStats.getTotalRxBytes()
        val curTotalTxBytes = TrafficStats.getTotalTxBytes()
        val curMobileRxBytes = TrafficStats.getMobileRxBytes()
        val curMobileTxBytes = TrafficStats.getMobileTxBytes()

        val prevTime = timestamp.getAndSet(curTime)
        val prevTotalRxBytes = totalRxBytes.getAndSet(curTotalRxBytes)
        val prevTotalTxBytes = totalTxBytes.getAndSet(curTotalTxBytes)
        val prevMobileRxBytes = mobileRxBytes.getAndSet(curMobileRxBytes)
        val prevMobileTxBytes = mobileTxBytes.getAndSet(curMobileTxBytes)

        if (prevTime > 0) {
            val netTotalRxBytes = curTotalRxBytes - prevTotalRxBytes
            val netTotalTxBytes = curTotalTxBytes - prevTotalTxBytes
            val netMobileRxBytes = curMobileRxBytes - prevMobileRxBytes
            val netMobileTxBytes = curMobileTxBytes - prevMobileTxBytes

            DataTrafficEntity(
                    fromTime = prevTime,
                    rxBytes = netTotalRxBytes,
                    txBytes = netTotalTxBytes,
                    mobileRxBytes = netMobileRxBytes,
                    mobileTxBytes = netMobileTxBytes
            ).fill(timeMillis = curTime).also { entity ->
                ObjBox.put(entity)
                setStatus(Status(lastTime = curTime))
            }
        }
    }

    private val dataListener by lazy {
        object : PhoneStateListener() {

            override fun onDataActivity(direction: Int) {
                super.onDataActivity(direction)
                if (direction in directions) {
                    GlobalScope.launch { handleDataActivity() }
                }
            }
        }
    }

    override suspend fun onStart() {
        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_DATA_ACTIVITY)
    }

    override suspend fun onStop() {
        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_NONE)
    }

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent?
        get() = null

    override suspend fun checkAvailability(): Boolean = true
}