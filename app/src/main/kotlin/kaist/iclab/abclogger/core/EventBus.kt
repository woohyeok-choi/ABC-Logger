package kaist.iclab.abclogger.core

import org.greenrobot.eventbus.EventBus as GreenRobotEventBus

interface ToEvent {
    fun toEvent(any: Any): Event
}

object EventBus: ToEvent {
    fun post(any: Any) {
        GreenRobotEventBus.getDefault().post(toEvent(any))
    }

    fun register(subscriber: Any) {
        if (!GreenRobotEventBus.getDefault().isRegistered(subscriber)) GreenRobotEventBus.getDefault().register(subscriber)
    }

    fun unregister(subscriber: Any) {
        if (GreenRobotEventBus.getDefault().isRegistered(subscriber)) GreenRobotEventBus.getDefault().unregister(subscriber)
    }

    /**
     * Here, change any into appropriate Event.
     */
    override fun toEvent(any: Any): Event = Event(0, "")
}

data class Event(
    val timestamp: Long,
    val type: String
)