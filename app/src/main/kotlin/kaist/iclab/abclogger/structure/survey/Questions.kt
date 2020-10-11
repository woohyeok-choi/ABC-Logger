package kaist.iclab.abclogger.structure.survey

import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

data class Question(
        val title: AltText = AltText(),
        val isOtherShown: Boolean = false,
        val option: Option = NoneOption()
) {
    companion object {
        val Factory: PolymorphicJsonAdapterFactory<Option> = PolymorphicJsonAdapterFactory.of(Option::class.java, "type")
            .withSubtype(FreeTextOption::class.java, Option.Type.FREE_TEXT.name)
            .withSubtype(RadioButtonOption::class.java, Option.Type.RADIO_BUTTON.name)
            .withSubtype(CheckBoxOption::class.java, Option.Type.CHECK_BOX.name)
            .withSubtype(DropdownOption::class.java, Option.Type.DROPDOWN.name)
            .withSubtype(SliderOption::class.java, Option.Type.SLIDER.name)
            .withSubtype(RangeOption::class.java, Option.Type.RANGE.name)
            .withSubtype(LinearScaleOption::class.java, Option.Type.LINEAR_SCALE.name)
            .withDefaultValue(NoneOption())
    }
}

sealed class Option(val type: Type) {
    enum class Type {
        NONE,
        FREE_TEXT,
        RADIO_BUTTON,
        CHECK_BOX,
        SLIDER,
        RANGE,
        LINEAR_SCALE,
        DROPDOWN
    }
}

class NoneOption : Option(Type.NONE)

data class FreeTextOption(
        val isSingleLine: Boolean = true
) : Option(Type.FREE_TEXT)

data class RadioButtonOption(
        val isVertical: Boolean = true,
        val items: Array<String> = arrayOf()
) : Option(Type.RADIO_BUTTON) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RadioButtonOption

        if (isVertical != other.isVertical) return false
        if (!items.contentEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isVertical.hashCode()
        result = 31 * result + items.contentHashCode()
        return result
    }
}

data class CheckBoxOption(
        val isVertical: Boolean = true,
        val items: Array<String> = arrayOf()
) : Option(Type.CHECK_BOX) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CheckBoxOption

        if (!items.contentEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        return items.contentHashCode()
    }
}

data class DropdownOption(
        val items: Array<String> = arrayOf()
) : Option(Type.DROPDOWN) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DropdownOption

        if (!items.contentEquals(other.items)) return false

        return true
    }

    override fun hashCode(): Int {
        return items.contentHashCode()
    }
}

data class SliderOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F
) : Option(Type.SLIDER)

data class RangeOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F
) : Option(Type.RANGE)

data class LinearScaleOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F,
        val defaultValue: Float = min,
        val minLabel: String = min.toString(),
        val maxLabel: String = max.toString()
) : Option(Type.LINEAR_SCALE)
