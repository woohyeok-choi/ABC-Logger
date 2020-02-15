package kaist.iclab.abclogger.collector.traffic

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import kaist.iclab.abclogger.ObjBox
import kaist.iclab.abclogger.R
import kaist.iclab.abclogger.collector.BaseCollector
import kaist.iclab.abclogger.collector.BaseStatus
import kaist.iclab.abclogger.collector.fill
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

class DataTrafficCollector(private val context: Context) : BaseCollector<DataTrafficCollector.Status>(context) {
    data class Status(override val hasStarted: Boolean? = null,
                      override val lastTime: Long? = null) : BaseStatus() {
        override fun info(): Map<String, Any> = mapOf()
    }

    override val clazz: KClass<Status> = Status::class

    override val name: String = context.getString(R.string.data_name_traffic)

    override val description: String = context.getString(R.string.data_desc_traffic)

    override val requiredPermissions: List<String> = listOf()

    override val newIntentForSetUp: Intent? = null

    override suspend fun checkAvailability(): Boolean = true

    override suspend fun onStart() {
        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_DATA_ACTIVITY)
    }

    override suspend fun onStop() {
        telephonyManager.listen(dataListener, PhoneStateListener.LISTEN_NONE)
    }

    private val telephonyManager: TelephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    private val dataListener = object : PhoneStateListener() {
        override fun onDataActivity(direction: Int) {
            super.onDataActivity(direction)
            if (direction in directions) {
                handleTrafficRetrieval()
            }
        }
    }

    private val timestamp: AtomicLong = AtomicLong(0)
    private val totalRxBytes: AtomicLong = AtomicLong(0)
    private val totalTxBytes: AtomicLong = AtomicLong(0)
    private val mobileRxBytes: AtomicLong = AtomicLong(0)
    private val mobileTxBytes: AtomicLong = AtomicLong(0)

    private val directions = arrayOf(
            TelephonyManager.DATA_ACTIVITY_IN,
            TelephonyManager.DATA_ACTIVITY_OUT,
            TelephonyManager.DATA_ACTIVITY_INOUT
    )

    private fun handleTrafficRetrieval() = launch {
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
                    toTime = curTime,
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


}