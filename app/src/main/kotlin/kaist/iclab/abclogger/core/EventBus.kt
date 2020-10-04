package kaist.iclab.abclogger

import org.greenrobot.eventbus.EventBus as GreenRobotEventBus

object EventBus {
    fun <T> post(entity: T) {
        GreenRobotEventBus.getDefault().post(entity)
    }

    fun register(subscriber: Any) {
        GreenRobotEventBus.getDefault().register(subscriber)
    }

    fun unregister(subscriber: Any) {
        GreenRobotEventBus.getDefault().unregister(subscriber)
    }
}

data class Event(
    val timestamp: Long,
    val type: String
)