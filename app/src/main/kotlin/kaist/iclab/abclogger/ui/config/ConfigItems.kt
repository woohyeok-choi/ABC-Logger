package kaist.iclab.abclogger.ui.config

open class ConfigData(open val title: String)

data class ConfigHeader(override val title: String) : ConfigData(title)

class ConfigHeaderBuilder {
    var title: String = ""

    fun build(): ConfigHeader = ConfigHeader(title)
}


open class ConfigItem(
        override val title: String,
        open val description: String
) : ConfigData(title)

data class SimpleConfigItem(
        override val title: String,
        override val description: String,
        var onAction: (() -> Unit)? = null
) : ConfigItem(title, description) {
    class Builder {
        var title: String = ""
        var description: String = ""
        var onAction: (() -> Unit)? = null

        fun build(): SimpleConfigItem = SimpleConfigItem(title, description, onAction)
    }
}

data class SwitchConfigItem(
        override val title: String,
        override val description: String,
        val isChecked: Boolean = false,
        var onChange: ((Boolean) -> Unit)? = null
) : ConfigItem(title, description) {
    class Builder {
        var title: String = ""
        var description: String = ""
        var isChecked: Boolean = false
        var onChange: ((Boolean) -> Unit)? = null

        fun build(): SwitchConfigItem = SwitchConfigItem(title, description, isChecked, onChange)
    }
}

data class DataConfigItem(
        override val title: String,
        override val description: String,
        val isChecked: Boolean = false,
        val isAvailable: Boolean = false,
        val info: String = "",
        val onAction: (() -> Unit)? = null,
        val onChange: ((Boolean) -> Unit)? = null
) : ConfigItem(title, description) {
    class Builder {
        var title: String = ""
        var description: String = ""
        var isChecked: Boolean = false
        var isAvailable: Boolean = false
        var info: String = ""
        var onAction: (() -> Unit)? = null
        var onChange: ((Boolean) -> Unit)? = null

        fun build(): DataConfigItem = DataConfigItem(
                title, description, isChecked, isAvailable, info, onAction, onChange
        )
    }
}

/**
 * DSL for build configuration
 */

class Configs : ArrayList<ConfigData>() {
    suspend fun header(init: suspend ConfigHeaderBuilder.() -> Unit) {
        val builder = ConfigHeaderBuilder()
        builder.init()
        add(builder.build())
    }

    suspend fun simple(init: suspend SimpleConfigItem.Builder.() -> Unit) {
        val builder = SimpleConfigItem.Builder()
        builder.init()
        add(builder.build())
    }

    suspend fun switch(init: suspend SwitchConfigItem.Builder.() -> Unit) {
        val builder = SwitchConfigItem.Builder()
        builder.init()
        add(builder.build())
    }

    suspend fun data(init: suspend DataConfigItem.Builder.() -> Unit) {
        val builder = DataConfigItem.Builder()
        builder.init()
        add(builder.build())
    }
}

suspend fun configs(init: suspend Configs.() -> Unit): ArrayList<ConfigData> {
    val c = Configs()
    c.init()
    return c
}
