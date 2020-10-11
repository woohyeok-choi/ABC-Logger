package kaist.iclab.abclogger.structure.survey

data class AltText(
    val main: String = "",
    val alt: String = ""
) {
    fun text(isAltTextShown: Boolean) = if (isAltTextShown && alt.isNotBlank()) alt else main
}