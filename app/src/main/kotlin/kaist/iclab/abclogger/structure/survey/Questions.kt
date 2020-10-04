package kaist.iclab.abclogger.structure.survey

import android.os.Parcel
import android.os.Parcelable
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

data class Question(
        val title: AltText = AltText(),
        val isOtherShown: Boolean = false,
        val option: Option = NoneOption()
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(AltText::class.java.classLoader) ?: AltText(),
        parcel.readByte() != 0.toByte(),
        parcel.readParcelable(Option::class.java.classLoader) ?: NoneOption()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(title, flags)
        parcel.writeByte(if (isOtherShown) 1 else 0)
        parcel.writeParcelable(option, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Question> {
        val Factory: PolymorphicJsonAdapterFactory<Option> = PolymorphicJsonAdapterFactory.of(Option::class.java, "type")
            .withSubtype(FreeTextOption::class.java, Option.Type.FREE_TEXT.name)
            .withSubtype(RadioButtonOption::class.java, Option.Type.RADIO_BUTTON.name)
            .withSubtype(CheckBoxOption::class.java, Option.Type.CHECK_BOX.name)
            .withSubtype(DropdownOption::class.java, Option.Type.DROPDOWN.name)
            .withSubtype(SliderOption::class.java, Option.Type.SLIDER.name)
            .withSubtype(RangeOption::class.java, Option.Type.RANGE.name)
            .withDefaultValue(NoneOption())

        override fun createFromParcel(parcel: Parcel): Question {
            return Question(parcel)
        }

        override fun newArray(size: Int): Array<Question?> {
            return arrayOfNulls(size)
        }
    }
}

sealed class Option(val type: Type) : Parcelable {
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(type.name)
    }

    override fun describeContents(): Int = 0
}

class NoneOption : Option(Type.NONE) {
    companion object CREATOR : Parcelable.Creator<NoneOption> {
        override fun createFromParcel(parcel: Parcel): NoneOption {
            return NoneOption()
        }

        override fun newArray(size: Int): Array<NoneOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class FreeTextOption(
        val isSingleLine: Boolean = true
) : Option(Type.FREE_TEXT) {
    constructor(parcel: Parcel) : this(parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeByte(if (isSingleLine) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<FreeTextOption> {
        override fun createFromParcel(parcel: Parcel): FreeTextOption {
            return FreeTextOption(parcel)
        }

        override fun newArray(size: Int): Array<FreeTextOption?> {
            return arrayOfNulls(size)
        }
    }
}


data class RadioButtonOption(
        val isVertical: Boolean = true,
        val items: Array<String> = arrayOf()
) : Option(Type.RADIO_BUTTON) {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.createStringArray() ?: arrayOf()
    )

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeByte(if (isVertical) 1 else 0)
        parcel.writeStringArray(items)
    }

    companion object CREATOR : Parcelable.Creator<RadioButtonOption> {
        override fun createFromParcel(parcel: Parcel): RadioButtonOption {
            return RadioButtonOption(parcel)
        }

        override fun newArray(size: Int): Array<RadioButtonOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class CheckBoxOption(
        val isVertical: Boolean = true,
        val items: Array<String> = arrayOf()
) : Option(Type.CHECK_BOX) {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.createStringArray() ?: arrayOf()
    )

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeByte(if (isVertical) 1 else 0)
        parcel.writeStringArray(items)
    }

    companion object CREATOR : Parcelable.Creator<CheckBoxOption> {
        override fun createFromParcel(parcel: Parcel): CheckBoxOption {
            return CheckBoxOption(parcel)
        }

        override fun newArray(size: Int): Array<CheckBoxOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class DropdownOption(
        val items: Array<String> = arrayOf()
) : Option(Type.DROPDOWN) {
    constructor(parcel: Parcel) : this(parcel.createStringArray() ?: arrayOf())

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeStringArray(items)
    }

    companion object CREATOR : Parcelable.Creator<DropdownOption> {
        override fun createFromParcel(parcel: Parcel): DropdownOption {
            return DropdownOption(parcel)
        }

        override fun newArray(size: Int): Array<DropdownOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class SliderOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F
) : Option(Type.SLIDER) {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(min)
        parcel.writeFloat(max)
        parcel.writeFloat(step)
    }

    companion object CREATOR : Parcelable.Creator<SliderOption> {
        override fun createFromParcel(parcel: Parcel): SliderOption {
            return SliderOption(parcel)
        }

        override fun newArray(size: Int): Array<SliderOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class RangeOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F
) : Option(Type.RANGE) {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(min)
        parcel.writeFloat(max)
        parcel.writeFloat(step)
    }

    companion object CREATOR : Parcelable.Creator<RangeOption> {
        override fun createFromParcel(parcel: Parcel): RangeOption {
            return RangeOption(parcel)
        }

        override fun newArray(size: Int): Array<RangeOption?> {
            return arrayOfNulls(size)
        }
    }
}

data class LinearScaleOption(
        val min: Float = 0F,
        val max: Float = 100F,
        val step: Float = 1F,
        val defaultValue: Float = min,
        val minLabel: String = min.toString(),
        val maxLabel: String = max.toString()
) : Option(Type.LINEAR_SCALE) {
    constructor(parcel: Parcel) : this(
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        super.writeToParcel(parcel, flags)
        parcel.writeFloat(min)
        parcel.writeFloat(max)
        parcel.writeFloat(step)
        parcel.writeFloat(defaultValue)
        parcel.writeString(minLabel)
        parcel.writeString(maxLabel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LinearScaleOption> {
        override fun createFromParcel(parcel: Parcel): LinearScaleOption {
            return LinearScaleOption(parcel)
        }

        override fun newArray(size: Int): Array<LinearScaleOption?> {
            return arrayOfNulls(size)
        }
    }
}
