package kaist.iclab.abclogger.structure.config

import android.text.InputType
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContract
import androidx.databinding.BaseObservable
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import kaist.iclab.abclogger.commons.getActivityResult
import kaist.iclab.abclogger.core.collector.Status
import kaist.iclab.abclogger.dialog.VersatileDialog
import kaist.iclab.abclogger.view.StatusColor
import org.koin.core.qualifier.named

private const val EMPTY_VALUE = -1

interface ConfigData {
    val name: String
}

data class ConfigHeader(override val name: String) : ConfigData

abstract class ConfigItem<T : Any>(
    override val name: String,
    protected val default: T,
    protected val format: (T) -> String,
    protected val color: (T) -> StatusColor
) : ConfigData, BaseObservable() {
    abstract override fun toString(): String
    abstract fun statusColor(): StatusColor

    abstract class Builder<T : Any, I : ConfigItem<T>>(val name: String, val default: T) {
        var format: ((T) -> String) = { it.toString() }
        var color: (T) -> StatusColor = { StatusColor.NONE }

        abstract fun build(): I
    }
}

class ReadOnlyConfigItem private constructor(
    name: String,
    format: (Any) -> String,
    statusColor: (Any) -> StatusColor
) : ConfigItem<Any>(name, EMPTY_VALUE, format, statusColor) {
    override fun toString(): String = format.invoke(default)

    override fun statusColor(): StatusColor = color.invoke(default)

    class Builder(name: String) : ConfigItem.Builder<Any, ReadOnlyConfigItem>(name, EMPTY_VALUE) {
        override fun build(): ReadOnlyConfigItem = ReadOnlyConfigItem(name, format, color)
    }
}

abstract class ReadWriteConfigItem<T: Any>(
    name: String,
    default: T,
    format: (T) -> String,
    statusColor: (T) -> StatusColor,
    protected val onBeforeChange: ReadWriteConfigItem<T>.() -> Unit,
    protected val onAfterChange: ReadWriteConfigItem<T>.() -> Unit,
) : ConfigItem<T>(name, default, format, statusColor) {
    var value = default
        set(value) {
            if (field == value) return
            field = value
            notifyChange()
            onAfterChange.invoke(this)
        }

    override fun toString(): String = format.invoke(value)

    override fun statusColor(): StatusColor = color.invoke(value)

    abstract class Builder<T : Any, I : ReadWriteConfigItem<T>>(name: String, default: T): ConfigItem.Builder<T, I>(name, default){
        var onBeforeChange: ReadWriteConfigItem<T>.() -> Unit = { }
        var onAfterChange: ReadWriteConfigItem<T>.() -> Unit = { }
    }
}

class ActionableConfigItem<T : Any> private constructor(
    name: String,
    default: T,
    format: (T) -> String,
    statusColor: (T) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<T>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<T>.() -> Unit,
    private val action: (ActionableConfigItem<T>.() -> Unit),
    private val confirmMessage: String?
) : ReadWriteConfigItem<T>(name, default, format, statusColor, onBeforeChange, onAfterChange) {
    suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        onBeforeChange.invoke(this)

        if (!confirmMessage.isNullOrBlank()) {
            val isConfirmed = VersatileDialog.confirm(
                manager = fragmentManager,
                owner = lifecycleOwner,
                title = name,
                message = confirmMessage
            )
            if (!isConfirmed) return
        }

        action.invoke(this)
    }

    class Builder<T : Any>(name: String, default: T) :
        ReadWriteConfigItem.Builder<T, ActionableConfigItem<T>>(name, default) {
        var confirmMessage: String? = null
        var action: (ActionableConfigItem<T>.() -> Unit) = { }

        override fun build(): ActionableConfigItem<T> =
            ActionableConfigItem(name, default, format, color, onBeforeChange, onAfterChange, action, confirmMessage)
    }
}

class ActivityResultConfigItem<T : Any, I, O> private constructor(
    name: String,
    default: T,
    format: (T) -> String,
    statusColor: (T) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<T>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<T>.() -> Unit,
    private val input: I,
    private val contract: ActivityResultContract<I, O>,
    private val transform: (O) -> T,
    private val confirmMessage: String?,
) : ReadWriteConfigItem<T>(name, default, format, statusColor, onBeforeChange, onAfterChange) {
    suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner, caller: ActivityResultCaller) {
        if (input == null) return

        onBeforeChange.invoke(this)

        if (!confirmMessage.isNullOrBlank()) {
            val isConfirmed = VersatileDialog.confirm(
                manager = fragmentManager,
                owner = lifecycleOwner,
                title = name,
                message = confirmMessage
            )
            if (!isConfirmed) return
        }

        val output = caller.getActivityResult(input, contract)
        value = transform.invoke(output)
    }

    class Builder<T : Any, I, O>(
        name: String,
        default: T,
        private val input: I,
        private val contract: ActivityResultContract<I, O>,
        private val transform: (O) -> T,
    ) : ReadWriteConfigItem.Builder<T, ActivityResultConfigItem<T, I, O>>(name, default) {
        var confirmMessage: String? = null

        override fun build(): ActivityResultConfigItem<T, I, O> = ActivityResultConfigItem(
            name, default, format, color,
            onBeforeChange, onAfterChange, input, contract, transform, confirmMessage
        )
    }
}

class CollectorConfigItem private constructor(
    name: String,
    default: Status,
    format: (Status) -> String,
    statusColor: (Status) -> StatusColor,
    onAfterChange: ReadWriteConfigItem<Status>.() -> Unit,
    val qualifiedName: String,
    val description: String?
) : ReadWriteConfigItem<Status>(name, default, format, statusColor, { }, onAfterChange) {
    class Builder(name: String, default: Status, private val qualifiedName: String) :
        ReadWriteConfigItem.Builder<Status, CollectorConfigItem>(name, default) {
        var description: String? = null

        override fun build(): CollectorConfigItem =
            CollectorConfigItem(name, default, format, color, onAfterChange, qualifiedName, description)
    }
}

class RadioConfigItem<T : Any> private constructor(
    name: String,
    default: T,
    format: (T) -> String,
    statusColor: (T) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<T>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<T>.() -> Unit,
    private val options: Array<T>,
    private val formatOption: ((T) -> String)? = null
) : ReadWriteConfigItem<T>(name, default, format, statusColor, onBeforeChange, onAfterChange) {
    var index: Int
        get() = options.indexOf(value)
        set(index) {
            value = options.getOrNull(index) ?: value
        }

    suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        onBeforeChange.invoke(this)

        val result = VersatileDialog.singleChoice(
            manager = fragmentManager,
            owner = lifecycleOwner,
            title = name,
            value = index,
            items = options.map { formatOption?.invoke(it) ?: format.invoke(it) }.toTypedArray()
        )

        if (result != null) index = result
    }

    class Builder<T : Any>(name: String, default: T, private val options: Array<T>) :
        ReadWriteConfigItem.Builder<T, RadioConfigItem<T>>(name, default) {

        var formatOption: ((T) -> String)? = null

        override fun build(): RadioConfigItem<T> =
            RadioConfigItem(name, default, format, color, onBeforeChange, onAfterChange, options, formatOption)
    }
}

class NumberConfigItem private constructor(
    name: String,
    default: Float,
    format: (Float) -> String,
    statusColor: (Float) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<Float>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<Float>.() -> Unit,
    private val min: Float,
    private val max: Float,
    private val step: Float,
    private val formatOption: ((Float) -> String)?
) : ReadWriteConfigItem<Float>(name, default, format, statusColor, onBeforeChange, onAfterChange) {

    suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        onBeforeChange.invoke(this)

        val result = VersatileDialog.slider(
            manager = fragmentManager,
            owner = lifecycleOwner,
            title = name,
            value = value,
            from = min,
            to = max,
            step = step,
            labelFormatter = formatOption ?: format
        )
        if (result != null) value = result
    }

    class Builder(name: String, default: Float) :
        ReadWriteConfigItem.Builder<Float, NumberConfigItem>(name, default) {
        var min = 0F
        var max = 100F
        var step = 1F
        var formatOption: ((Float) -> String)? = null

        override fun build(): NumberConfigItem = NumberConfigItem(
            name, default, format, color,
            onBeforeChange, onAfterChange, min, max, step, formatOption
        )
    }
}

class NumberRangeConfigItem private constructor(
    name: String,
    default: Pair<Float, Float>,
    private val formatEach: (Float) -> String,
    statusColor: (Pair<Float, Float>) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<Pair<Float, Float>>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<Pair<Float, Float>>.() -> Unit,
    private val min: Float,
    private val max: Float,
    private val step: Float,
    private val formatOption: ((Float) -> String)?
) : ReadWriteConfigItem<Pair<Float, Float>>(
    name,
    default,
    { (from, to) ->
        "${formatEach.invoke(from)} - ${formatEach.invoke(to)}"
    },
    statusColor,
    onBeforeChange,
    onAfterChange
) {
     suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        onBeforeChange.invoke(this)

        val result = VersatileDialog.sliderRange(
            manager = fragmentManager,
            owner = lifecycleOwner,
            title = name,
            value = value,
            from = min,
            to = max,
            step = step,
            labelFormatter = formatOption ?: formatEach
        )
        if (result != null) value = result
    }

    class Builder(name: String, default: Pair<Float, Float>) :
        ReadWriteConfigItem.Builder<Pair<Float, Float>, NumberRangeConfigItem>(name, default) {
        var min = 0F
        var max = 100F
        var step = 1F
        var formatEach: ((Float) -> String) = { it.toString() }
        var formatOption: ((Float) -> String)? = null

        override fun build(): NumberRangeConfigItem = NumberRangeConfigItem(
            name, default, formatEach, color,
            onBeforeChange, onAfterChange, min, max, step, formatOption
        )
    }
}

class TextConfigItem(
    name: String,
    default: String,
    format: (String) -> String,
    statusColor: (String) -> StatusColor,
    onBeforeChange: ReadWriteConfigItem<String>.() -> Unit,
    onAfterChange: ReadWriteConfigItem<String>.() -> Unit,
    private val inputType: Int,
) : ReadWriteConfigItem<String>(name, default, format, statusColor, onBeforeChange, onAfterChange) {
    suspend fun run(fragmentManager: FragmentManager, lifecycleOwner: LifecycleOwner) {
        onBeforeChange.invoke(this)

        val result = VersatileDialog.text(
            manager = fragmentManager,
            owner = lifecycleOwner,
            title = name,
            value = value,
            inputType = inputType
        )

        if (result != null) value = result
    }

    class Builder(name: String, default: String) :
        ReadWriteConfigItem.Builder<String, TextConfigItem>(name, default) {
        var inputType: Int = InputType.TYPE_CLASS_TEXT

        override fun build(): TextConfigItem =
            TextConfigItem(name, default, format, color, onBeforeChange, onAfterChange, inputType)
    }
}
