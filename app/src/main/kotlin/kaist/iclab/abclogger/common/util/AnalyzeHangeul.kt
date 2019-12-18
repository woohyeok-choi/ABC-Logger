package kaist.iclab.abclogger.common.util


object AnalyzeHangeul {

    // ㄱ             ㄲ            ㄴ               ㄷ            ㄸ             ㄹ
    // ㅁ             ㅂ            ㅃ               ㅅ            ㅆ             ㅇ
    // ㅈ             ㅉ           ㅊ                ㅋ            ㅌ              ㅍ      ㅎ
    internal val ChoSung = charArrayOf(0x3131.toChar(), 0x3132.toChar(), 0x3134.toChar(), 0x3137.toChar(), 0x3138.toChar(), 0x3139.toChar(), 0x3141.toChar(), 0x3142.toChar(), 0x3143.toChar(), 0x3145.toChar(), 0x3146.toChar(), 0x3147.toChar(), 0x3148.toChar(), 0x3149.toChar(), 0x314a.toChar(), 0x314b.toChar(), 0x314c.toChar(), 0x314d.toChar(), 0x314e.toChar())

    internal val ChoSungEng = arrayOf("r", "R", "s", "e", "E", "f", "a", "q", "Q", "t", "T", "d", "w", "W", "c", "z", "x", "v", "g")

    // ㅏ            ㅐ             ㅑ             ㅒ            ㅓ             ㅔ
    // ㅕ            ㅖ              ㅗ           ㅘ            ㅙ              ㅚ
    // ㅛ            ㅜ              ㅝ           ㅞ             ㅟ             ㅠ
    // ㅡ           ㅢ              ㅣ
    internal val JwungSung = charArrayOf(0x314f.toChar(), 0x3150.toChar(), 0x3151.toChar(), 0x3152.toChar(), 0x3153.toChar(), 0x3154.toChar(), 0x3155.toChar(), 0x3156.toChar(), 0x3157.toChar(), 0x3158.toChar(), 0x3159.toChar(), 0x315a.toChar(), 0x315b.toChar(), 0x315c.toChar(), 0x315d.toChar(), 0x315e.toChar(), 0x315f.toChar(), 0x3160.toChar(), 0x3161.toChar(), 0x3162.toChar(), 0x3163.toChar())

    internal val JwungSungEng = arrayOf("k", "o", "i", "O", "j", "p", "u", "P", "h", "hk", "ho", "hl", "y", "n", "nj", "np", "nl", "b", "m", "ml", "l")

    //         ㄱ            ㄲ             ㄳ            ㄴ              ㄵ
    // ㄶ             ㄷ            ㄹ             ㄺ            ㄻ              ㄼ
    // ㄽ             ㄾ            ㄿ              ㅀ            ㅁ             ㅂ
    // ㅄ            ㅅ             ㅆ             ㅇ            ㅈ             ㅊ
    // ㅋ            ㅌ            ㅍ              ㅎ
    internal val JongSung = charArrayOf(0.toChar(), 0x3131.toChar(), 0x3132.toChar(), 0x3133.toChar(), 0x3134.toChar(), 0x3135.toChar(), 0x3136.toChar(), 0x3137.toChar(), 0x3139.toChar(), 0x313a.toChar(), 0x313b.toChar(), 0x313c.toChar(), 0x313d.toChar(), 0x313e.toChar(), 0x313f.toChar(), 0x3140.toChar(), 0x3141.toChar(), 0x3142.toChar(), 0x3144.toChar(), 0x3145.toChar(), 0x3146.toChar(), 0x3147.toChar(), 0x3148.toChar(), 0x314a.toChar(), 0x314b.toChar(), 0x314c.toChar(), 0x314d.toChar(), 0x314e.toChar())

    internal val JongSungEng = arrayOf("", "r", "R", "rt", "s", "sw", "sg", "e", "f", "fr", "fa", "fq", "ft", "fx", "fv", "fg", "a", "q", "qt", "t", "T", "d", "w", "c", "z", "x", "v", "g")


    fun hangulToJaso(s: String): String {

        var a: Int
        var b: Int
        var c: Int // 자소 버퍼: 초성/중성/종성 순
        var result = ""

        for (i in 0 until s.length) {
            val ch = s[i]

            if (ch.toInt() >= 0xAC00 && ch.toInt() <= 0xD7A3) { // "AC00:가" ~ "D7A3:힣" 에 속한 글자면 분해
                c = ch.toInt() - 0xAC00
                a = c / (21 * 28)
                c = c % (21 * 28)
                b = c / 28
                c = c % 28

                result = result + ChoSung[a] + JwungSung[b]
                if (c != 0) result = result + JongSung[c] // c가 0이 아니면, 즉 받침이 있으면
            } else {
                result = result + ch
            }
        }
        return result
    }


    /**
     * 한글기준의 문자열을 입력받아서 한글의 경우에는 영타기준으로 변경한다.
     * @param s 한글/영문/특수문자가 합쳐진 문자열
     * @return 영타기준으로 변경된 문자열값
     */
    fun convertToEnglish(s: String): String {
        // *****************************************
        // 0xAC00 + ( (초성순서 * 21) + 중성순서 ) * 28 + 종성순서 = 한글유니코드값
        // ( (초성순서 * 21) + 중성순서 ) * 28 + 종성순서 = 순수한글코드
        // 순수한글코드 % 28 = 종성
        // ( (순수한글코드 - 종성) / 28 ) % 21 = 중성
        // ( ( ( 순수한글코드 - 종성) / 28) - 중성) ) / 21 = 초성
        // *******************************************

        var a: Int
        var b: Int
        var c: Int // 자소 버퍼: 초성/중성/종성 순
        var result = ""

        for (i in 0 until s.length) {
            val ch = s[i]

            if (ch.toInt() >= 0xAC00 && ch.toInt() <= 0xD7A3) { // "AC00:가" ~ "D7A3:힣" 에 속한 글자면 분해
                c = ch.toInt() - 0xAC00
                a = c / (21 * 28)
                c = c % (21 * 28)
                b = c / 28
                c = c % 28

                result = result + ChoSungEng[a] + JwungSungEng[b]

                if (c != 0) result = result + JongSungEng[c] // c가 0이 아니면, 즉 받침이 있으면
            } else {
                result = result + ch
            }
        }

        return result
    }

    /*
     * 완성되지 않은 한글의 경우 영문 변환이 제대로 되지 않는다.
     * 잘못된 글자인 경우에도 영문으로 변환이 가능하도록 추가적으로 처리하는 함수
     * 글자가 초성, 중성, 종성을 구성하는 글자 배열을 루프돌면서 같은글자가 있는지
     * 확인한 후 해당 영문으로 변환함.
     */
    fun convertToEnglishforSingleChar(s: String): String {
        var result = ""
        var temp: String? = null

        for (i in 0 until s.length) {
            val ch = s[i]

            if (ch.toInt() >= 0x3131 && ch.toInt() <= 0x3163) {
                temp = findChoSung(ch)
                if (temp != null) {
                    result = result + temp
                } else {
                    temp = findJwungSung(ch)
                    if (temp != null) {
                        result = result + temp
                    } else {
                        temp = findJongSung(ch)
                        if (temp != null) {
                            result = result + temp
                        } else {
                            result = result + ch
                        }
                    }
                }
            } else {
                result = result + ch
            }

        }

        return result
    }

    private fun findChoSung(c: Char): String? {
        var result: String? = null
        for (i in ChoSung.indices) {
            if (ChoSung[i] == c) {
                result = ChoSungEng[i]
                break
            }
        }
        return result
    }

    private fun findJwungSung(c: Char): String? {
        var result: String? = null
        for (i in JwungSung.indices) {
            if (JwungSung[i] == c) {
                result = JwungSungEng[i]
                break
            }
        }
        return result
    }

    private fun findJongSung(c: Char): String? {
        var result: String? = null
        for (i in JongSung.indices) {
            if (JongSung[i] == c) {
                result = JongSungEng[i]
                break
            }
        }
        return result
    }
}