package kaist.iclab.abclogger.common.type

class KeyObject(var _appName: String, var _inputChar: String, isChunjin: Boolean) {
    private val c_key = arrayOf("l", "ㆍ", "ㅡ", "backspace", "ㄱ", "ㅋ", "ㄴ", "ㄹ", "ㄷ", "ㅌ", "ㅂ", "ㅍ", "ㅅ", "ㅎ", "ㅈ", "ㅊ", ".", "?", "!", "ㅇ", "ㅁ", " ", "@", "ㅏ", "ㅑ", "ㅓ", "ㅕ", "ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "ㅣ", "ㅐ", "ㅔ", "ㅒ", "ㅖ", "ㅙ", "ㅞ", "ㅘ", "ㅝ", "ㅚ", "ㅟ", "ㅢ")

    private val c_coordinateX = floatArrayOf(1f, 2f, 3f, 4f, 1f, 1f, 2f, 2f, 3f, 3f, 1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f, 4f, 2f, 2f, 3f, 4f, 2f, 2f, 1f, 1f, 3f, 3f, 2f, 2f, 3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f)

    private val c_coordinateY = floatArrayOf(1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)

    private val q_kor_key = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ", "ㅃ", "ㅉ", "ㄸ", "ㄲ", "ㅆ", "ㅒ", "ㅖ", "ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ", "ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ", "backspace", "@", " ", ".")

    private val q_eng_key = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "g", "h", "j", "k", "l", "z", "x", "c", "v", "b", "n", "m", "backspace", "@", " ", ".")

    private val q_kor_coordinateX = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 8f, 9f, 1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 3f, 6f, 8.5f)

    private val q_kor_coordinateY = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 5f, 5f, 5f)


    private val q_eng_coordinateX = floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 3f, 6f, 8.5f)

    private val q_eng_coordinateY = floatArrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 5f, 5f, 5f)


    var posX: Float = 0.toFloat()
    var posY: Float = 0.toFloat()
    var eventTime: Long = 0
    var inputType: String
    var appName: String
    var keyDistance: String = ""
    var inputChar: String


    init {
        this.appName = _appName
        this.inputChar = _inputChar
        this.inputType = getInputCharType(_inputChar)
        this.eventTime = System.currentTimeMillis()
        this.posX = findPositionX(_inputChar, isChunjin)
        this.posY = findPositionY(_inputChar, isChunjin)
    }


    fun setupChunjin(isChunjin: Boolean) {
        this.posX = findPositionX(this.inputChar, isChunjin)
        this.posY = findPositionY(this.inputChar, isChunjin)
    }


    private fun findPositionX(ch: String, chunjinFlag: Boolean): Float {
        if (this.inputType == "H") {
            if (chunjinFlag) {
                for (i in c_key.indices) {
                    if (c_key[i] == ch)
                        return c_coordinateX[i]
                }
            } else {
                for (i in q_kor_key.indices) {
                    if (q_kor_key[i] == ch)
                        return q_kor_coordinateX[i]
                }
            }
        } else if (this.inputType == "E") {
            for (i in q_eng_key.indices) {
                if (q_eng_key[i] == ch)
                    return q_eng_coordinateX[i]
            }
        }
        return 0f
    }

    private fun findPositionY(ch: String, chunjinFlag: Boolean): Float {
        if (this.inputType == "H") {
            if (chunjinFlag) {
                for (i in c_key.indices) {
                    if (c_key[i] == ch)
                        return c_coordinateY[i]
                }
            } else {
                for (i in q_kor_key.indices) {
                    if (q_kor_key[i] == ch)
                        return q_kor_coordinateY[i]
                }
            }
        } else if (this.inputType == "E") {
            for (i in q_eng_key.indices) {
                if (q_eng_key[i] == ch)
                    return q_eng_coordinateY[i]
            }
        }

        return 0f
    }

    fun getInputCharType(str: String): String {
        return if (str.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*".toRegex()))
            "H"
        else if (str.matches("^[a-zA-Z]*$".toRegex()))
            "E"
        else if (str.matches("^[0-9]*$".toRegex()))
            "N"
        else
            "S"
    }


}