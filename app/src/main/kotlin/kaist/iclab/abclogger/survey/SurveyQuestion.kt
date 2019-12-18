package kaist.iclab.abclogger.survey

data class SurveyQuestion (
    val type: SurveyQuestionType,
    val shouldAnswers: Boolean = true,
    val text: String,
    val options: ArrayList<Any>? = arrayListOf(),
    val altText: String? = "",
    var response: ArrayList<String> = ArrayList(options?.size ?: 1)
)