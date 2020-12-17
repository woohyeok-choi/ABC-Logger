package kaist.iclab.abclogger.collector.traffic

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import kaist.iclab.abclogger.collector.event.DeviceEventEntity
import kaist.iclab.abclogger.core.collector.AbstractCollector
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.Description
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DataTrafficCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<DataTrafficEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    override fun isAvailable(): Boolean =
        TrafficStats.UNSUPPORTED !in listOf(
                TrafficStats.getMobileRxBytes(), TrafficStats.getMobileTxBytes(),
                TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes()
        ).map { it.toInt() }

    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = null

    override fun getDescription(): Array<Description> = arrayOf()

    private var job: Job? = null

    override suspend fun onStart() {
        val timestamp = AtomicLong(System.currentTimeMillis())
        val totalRxBytes = AtomicLong(TrafficStats.getTotalRxBytes())
        val totalTxBytes = AtomicLong(TrafficStats.getTotalTxBytes())
        val mobileRxBytes = AtomicLong(TrafficStats.getMobileRxBytes())
        val mobileTxBytes = AtomicLong(TrafficStats.getMobileTxBytes())

        if (job?.isActive == true) return

        job = launch {
            while (true) {
                delay(TimeUnit.SECONDS.toMillis(15))

                val curTimestamp = System.currentTimeMillis()
                val curTotalRxBytes = TrafficStats.getTotalRxBytes()
                val curTotalTxBytes = TrafficStats.getTotalTxBytes()
                val curMobileRxBytes = TrafficStats.getMobileRxBytes()
                val curMobileTxBytes = TrafficStats.getMobileTxBytes()

                val prevTimestamp = timestamp.getAndSet(curTimestamp)
                val prevTotalRxBytes = totalRxBytes.getAndSet(curTotalRxBytes)
                val prevTotalTxBytes = totalTxBytes.getAndSet(curTotalTxBytes)
                val prevMobileRxBytes = mobileRxBytes.getAndSet(curMobileRxBytes)
                val prevMobileTxBytes = mobileTxBytes.getAndSet(curMobileTxBytes)

                val netTotalRxBytes = curTotalRxBytes - prevTotalRxBytes
                val netTotalTxBytes = curTotalTxBytes - prevTotalTxBytes
                val netMobileRxBytes = curMobileRxBytes - prevMobileRxBytes
                val netMobileTxBytes = curMobileTxBytes - prevMobileTxBytes

                val entity = DataTrafficEntity(
                        fromTime = prevTimestamp,
                        toTime = curTimestamp,
                        rxBytes = netTotalRxBytes,
                        txBytes = netTotalTxBytes,
                        mobileRxBytes = netMobileRxBytes,
                        mobileTxBytes = netMobileTxBytes
                ).apply {
                    this.timestamp = curTimestamp
                }
                put(entity)
            }
        }
    }

    override suspend fun onStop() {
        job?.cancel()
    }

    override suspend fun count(): Long = dataRepository.count<DataTrafficEntity>()

    override suspend fun flush(entities: Collection<DataTrafficEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<DataTrafficEntity> = dataRepository.find(0, limit)
}