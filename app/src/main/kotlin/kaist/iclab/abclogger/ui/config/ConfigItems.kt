package kaist.iclab.abclogger.ui.config

import android.content.Intent

interface ConfigData

data class ConfigHeader(val title: String) : ConfigData

class ConfigHeaderBuilder {
    var title: String = ""

    fun build() : ConfigHeader = ConfigHeader(title)
}

abstract class ConfigItem (
    open val key: String,
    open val title: String,
    open val description: String
) : ConfigData

data class SimpleConfigItem(
        override val key: String,
        override val title: String,
        override val description: String
) : ConfigItem(key, title, description)

class SimpleConfigItemBuilder {
    var key : String = ""
    var title: String = ""
    var description: String = ""

    fun build() : SimpleConfigItem = SimpleConfigItem(key, title, description)
}

data class SwitchConfigItem(
        override val key: String,
        override val title: String,
        override val description: String,
        val isChecked: Boolean = false
) : ConfigItem(key, title, description)

class SwitchConfigItemBuilder {
    var key : String = ""
    var title: String = ""
    var description: String = ""
    var isChecked: Boolean = false

    fun build() : SwitchConfigItem = SwitchConfigItem(key, title, description, isChecked)
}

data class DataConfigItem(
        override val key: String,
        override val title: String,
        override val description: String,
        val isChecked: Boolean = false,
        val isAvailable: Boolean = false,
        val info: String = "",
        val intentForSetup: Intent? = null
) : ConfigItem(key, title, description)

class DataConfigItemBuilder {
    var key : String = ""
    var title: String = ""
    var description: String = ""
    var isChecked: Boolean = false
    var isAvailable: Boolean = false
    var info: String = ""
    var intent: Intent? = null

    fun build() : DataConfigItem = DataConfigItem(
            key, title, description, isChecked, isAvailable, info, intent
    )
}

/**
 * DSL for build configuration
 */

class Configs : ArrayList<ConfigData> () {
    suspend fun header(init: suspend ConfigHeaderBuilder.() -> Unit) {
        val builder = ConfigHeaderBuilder()
        builder.init()
        add(builder.build())
    }

    suspend fun simple(init: suspend SimpleConfigItemBuilder.() -> Unit) {
        val builder = SimpleConfigItemBuilder()
        builder.init()
        add(builder.build())
    }

    suspend fun switch(init: suspend SwitchConfigItemBuilder.() -> Unit) {
        val builder = SwitchConfigItemBuilder()
        builder.init()
        add(builder.build())
    }

    suspend fun data(init: suspend DataConfigItemBuilder.() -> Unit) {
        val builder = DataConfigItemBuilder()
        builder.init()
        add(builder.build())
    }
}

suspend fun configs(init: suspend Configs.() -> Unit) : ArrayList<ConfigData> {
    val c = Configs()
    c.init()
    return c
}
