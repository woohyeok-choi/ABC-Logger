package kaist.iclab.abclogger.structure.config

import androidx.activity.result.contract.ActivityResultContract
import kaist.iclab.abclogger.core.collector.Status

class ConfigCategoryBuilder(val name: String? = null) {
    private val configs: ArrayList<ConfigData> = if (name.isNullOrBlank()) {
        arrayListOf()
    } else {
        arrayListOf(ConfigHeader(name))
    }

    fun readonly(name: String, init: ReadOnlyConfigItem.Builder.() -> Unit) {
        val builder = ReadOnlyConfigItem.Builder(name)
        builder.init()
        configs.add(builder.build())
    }

    fun <T : Any> actionable(
        name: String,
        default: T,
        init: ActionableConfigItem.Builder<T>.() -> Unit
    ) {
        val builder = ActionableConfigItem.Builder(name, default)
        builder.init()
        configs.add(builder.build())
    }

    fun <T : Any, I, O> activity(
        name: String,
        default: T,
        input: I,
        contract: ActivityResultContract<I, O>,
        transform: (O) -> T,
        init: ActivityResultConfigItem.Builder<T, I, O>.() -> Unit
    ) {
        val builder = ActivityResultConfigItem.Builder(name, default, input, contract, transform)
        builder.init()
        configs.add(builder.build())
    }

    fun collector(
        name: String,
        status: Status,
        qualifiedName: String,
        init: CollectorConfigItem.Builder.() -> Unit
    ) {
        val builder = CollectorConfigItem.Builder(name, status, qualifiedName)
        builder.init()
        configs.add(builder.build())
    }

    fun <T : Any> radio(name: String, default: T, options: Array<T>, init: RadioConfigItem.Builder<T>.() -> Unit) {
        val builder = RadioConfigItem.Builder(name, default, options)
        builder.init()
        configs.add(builder.build())
    }

    fun number(name: String, default: Float, init: NumberConfigItem.Builder.() -> Unit) {
        val builder = NumberConfigItem.Builder(name, default)
        builder.init()
        configs.add(builder.build())
    }

    fun range(
        name: String,
        default: Pair<Float, Float>,
        init: NumberRangeConfigItem.Builder.() -> Unit
    ) {
        val builder = NumberRangeConfigItem.Builder(name, default)
        builder.init()
        configs.add(builder.build())
    }

    fun text(name: String, default: String, init: TextConfigItem.Builder.() -> Unit) {
        val builder = TextConfigItem.Builder(name, default)
        builder.init()
        configs.add(builder.build())
    }

    fun build(): ArrayList<ConfigData> = configs
}

class ConfigBuilder {
    private val categories = arrayListOf<ArrayList<ConfigData>>()

    fun category(name: String? = null, init: ConfigCategoryBuilder.() -> Unit) {
        val builder = ConfigCategoryBuilder(name)
        builder.init()
        categories.add(builder.build())
    }

    fun build(): Config = Config().apply {
        addAll(categories.flatten())
    }
}

class Config: ArrayList<ConfigData>()

fun config(init: ConfigBuilder.() -> Unit): Config {
    val config = ConfigBuilder()
    config.init()
    return config.build()
}
