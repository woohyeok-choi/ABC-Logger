package kaist.iclab.abclogger.collector

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kaist.iclab.abclogger.base.BaseCollector
import java.util.*

class KeyTrackingService : AccessibilityService(), BaseCollector {

    private var totalStr = ""
    private var isLocked = false
    val inputKeyObj = HashMap<Int, KeyObject>()
    internal var keyIdx = 0
    internal var keyType: String? = "E"
    internal var isChunjin: Boolean = false

    override fun onInterrupt() {}

    //    @Override
    //    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    //        String eventTypeStr = AccessibilityEvent.eventTypeToString(accessibilityEvent.getEventType());
    //        Log.i("AccessibilityService","-------------------------------");
    //        Log.i("AccessibilityService",eventTypeStr+".........");
    //        if(accessibilityEvent.getPackageName() != null) {
    //            String packageName = accessibilityEvent.getPackageName().toString();
    //            Log.i("AccessibilityService", "package name: "+ packageName);
    //        }
    //
    ////        final AccessibilityNodeInfo textNodeInfo = findTextViewNode(getRootInActiveWindow());
    ////        if (textNodeInfo == null) return;
    ////
    ////        Rect rect = new Rect();
    ////        textNodeInfo.getBoundsInScreen(rect);
    ////        Log.i("AA", "The TextView Node: " + rect.toString());
    //
    //
    //        List<AccessibilityWindowInfo> windows = getWindows();
    //        Log.i("AA", String.format("Windows (%d):", windows.size()));
    //        for (AccessibilityWindowInfo window : windows) {
    //            Log.i("AA", String.format("window: %s", window.toString()));
    //        }
    //
    //        /* Dump the view hierarchy */
    //        dumpNode(getRootInActiveWindow(), 0);
    //
    //
    //        Log.i("AccessibilityService","-------------------------------");
    //    }
    //
    //    private void dumpNode(AccessibilityNodeInfo node, int indent) {
    //        if (node == null) {
    //            Log.e("AA", "node is null (stopping iteration)");
    //            return;
    //        }
    //
    //        String indentStr = new String(new char[indent * 2]).replace('\0', ' ');
    //        Log.w("AA", String.format("%s NODE: %s", indentStr, node.toString()));
    //        for (int i = 0; i < node.getChildCount(); i++) {
    //            dumpNode(node.getChild(i), indent + 1);
    //        }
    //        /* NOTE: Not sure if this is really required. Documentation is unclear. */
    //        node.recycle();
    //    }


    override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
        val eventTypeStr = AccessibilityEvent.eventTypeToString(accessibilityEvent.eventType)
        /** 화면뷰 리소스 구하기: 전화 앱  */
        /*
        if(eventTypeStr.equals("TYPE_VIEW_CLICKED")) {
            Log.i("AccessibilityService","-------------------------------");
            Log.i("AccessibilityService",eventTypeStr+".........");
            Log.i("AccessibilityService",accessibilityEvent.getText()+".........");
            AccessibilityNodeInfo accessibilityNodeInfo = accessibilityEvent.getSource();
            trackingViewResources1(accessibilityNodeInfo);
            Log.i("AccessibilityService","-------------------------------");
        }
        */

        /** 키 입력 분석  */
        Log.i("AccessibilityService", "-------------------------------")
        if (eventTypeStr == "TYPE_VIEW_TEXT_CHANGED") isLocked = true
        if (eventTypeStr == "TYPE_VIEW_TEXT_SELECTION_CHANGED" && isLocked) {
            isLocked = false
            return
        }

        if (accessibilityEvent.packageName != null) {
            val packageName = accessibilityEvent.packageName.toString()
            //Log.w("AccessibilityService", "package name: $packageName")

            if (eventTypeStr == "TYPE_VIEW_TEXT_CHANGED" || eventTypeStr == "TYPE_VIEW_TEXT_SELECTION_CHANGED") {

                Log.i("AccessibilityService", "$eventTypeStr.........")
                val accessibilityNodeInfo = accessibilityEvent.source
                trackingViewResources2(accessibilityNodeInfo)
            }

            if (eventTypeStr == "TYPE_VIEW_FOCUSED") {
                //Log.i("AccessibilityService", eventTypeStr + ".........");
                val accessibilityNodeInfo = accessibilityEvent.source
                trackingViewResources3(accessibilityNodeInfo)
            }
        }
        Log.i("AccessibilityService", "-------------------------------")

    }


    private fun trackingViewResources1(parentView: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (parentView == null) return null
        if (parentView.viewIdResourceName != null)
            Log.w("AccessibilityService", "className: " + parentView.className + ", resourceName: " + parentView.viewIdResourceName + ", text: " + parentView.text)

        for (i in 0 until parentView.childCount) {
            val child = parentView.getChild(i)
            if (child != null)
                trackingViewResources1(child)
            else
                return null
        }

        return null
    }

    private fun trackingViewResources2(parentView: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (parentView == null) return null

        if (parentView.text != null && parentView.text.length >= 0 && parentView.className.toString().contains("EditText")) {
            //Log.e("AA", "text: "+parentView.getText() + parentView.getInputType());
            getCurrentInputChar(parentView.text, parentView.packageName)
        }

        for (i in 0 until parentView.childCount) {
            val child = parentView.getChild(i)
            if (child != null)
                trackingViewResources2(child)
            else
                return null
        }

        return null
    }


    private fun trackingViewResources3(parentView: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (parentView == null) return null

        if (parentView.text != null && parentView.text.length >= 0 && parentView.className.toString().contains("EditText")) {
            Log.e("AA", "focused text: " + parentView.text)
            totalStr = parentView.text.toString()
        }

        for (i in 0 until parentView.childCount) {
            val child = parentView.getChild(i)
            if (child != null)
                trackingViewResources2(child)
            else
                return null
        }

        return null
    }


    private fun getCurrentInputChar(str: CharSequence?, packageName: CharSequence) {

        if (str == null && totalStr.length > 0) {
            //Log.w("AccessibilityService", "입력 문자: backspace, 문자 타입: 특수문자")

            inputKeyObj[keyIdx++] = KeyObject(getAppNameByPackageName(applicationContext, packageName.toString()), "backspace", isChunjin)
            totalStr = ""
            isLocked = false
            return
        }
        val currentStr = str!!.toString()

        if (!currentStr.contains(totalStr) && !totalStr.contains(currentStr)) totalStr = ""


        //Log.w("AccessibilityService", "현재 문자열: $currentStr")
        //Log.w("AccessibilityService", "Total 문자열: $totalStr")
        val decomposeCurrentStr = hangulToJaso(currentStr)
        //Log.w("AccessibilityService", "Current 문자 분해: $decomposeCurrentStr")

        val decomposeTotalStr = hangulToJaso(totalStr)
        //Log.w("AccessibilityService", "Total 문자 분해: $decomposeTotalStr")

        /* 사용자가 새로운 키를 입력한 경우 */
        if (decomposeCurrentStr.length >= decomposeTotalStr.length && decomposeCurrentStr.length != 0) {

            val ch = decomposeCurrentStr.get(decomposeCurrentStr.length - 1) + ""
            //Log.w("AccessibilityService", "입력 문자: " + ch + ",  문자 타입: " + getInputCharType(ch))

            val currentKeyType = getInputCharType(ch)
            if (currentKeyType != keyType && keyType != null && ch != "ㆍ") {
                keyType = currentKeyType
                if (isChunjin) {
                    /* 천지인 좌표로 변환 */
                    for ((_, value) in inputKeyObj) {
                        value.setupChunjin(true)
                        //Log.d("AA", "글자: " + value.inputChar + ", posX: " + value.posX + ", posY: " + value.posY)
                    }
                }

                /* 터치 이동 거리 계산*/
                var idx = 0
                var prevPosX = 0f
                var prevPosY = 0f
                for ((key, value) in inputKeyObj) {
                    idx = key

                    if (idx == 0)
                        value.keyDistance = "0"
                    else
                        value.keyDistance = calculateDistance(value.posX, prevPosX, value.posY, prevPosY)

                    Log.d("AA", "글자: " + value.inputChar + ", posX: " + value.posX + ", posY: " + value.posY + ", distance: " + value.keyDistance)
                    /*서버에 저장*/
                    /**서버에 저장 */
                    val jsonEntity: String = "{\"InputTime\":"+value.eventTime+", \"AppName\":\""+value.appName+"\", \"keyType\":\""+value.inputType+"\", \"keyDistance\":"+ value.keyDistance +"}"

                    prevPosX = value.posX
                    prevPosY = value.posY
                    idx++
                }

                /* Map 초기화 */
                inputKeyObj.clear()
                keyIdx = 0
            }

            if (currentKeyType == "E") {
                isChunjin = false

                inputKeyObj[keyIdx++] = KeyObject(getAppNameByPackageName(applicationContext, packageName.toString()), ch, isChunjin)
            } else if (currentKeyType == "H") {
                inputKeyObj[keyIdx++] = KeyObject(getAppNameByPackageName(applicationContext, packageName.toString()), ch, isChunjin)
            } else if (currentKeyType == "S") {
                if (ch == "ㆍ") {
                    keyType = "H"
                    isChunjin = true
                }
            }

        } else if (decomposeCurrentStr.length < decomposeTotalStr.length || currentStr.length < totalStr.length) {
            //Log.w("AccessibilityService", "입력 문자: backspace, 문자 타입: 특수문자")
            inputKeyObj[keyIdx++] = KeyObject(getAppNameByPackageName(applicationContext, packageName.toString()), "backspace", isChunjin)
        }/* 사용자가 글자를 지운 경우: backspace 입력 */

        //printHashMap(inputKeyObj)

        totalStr = currentStr
    }

    private fun calculateDistance(posX: Float, prevPosX: Float, posY: Float, prevPosY: Float): String {
        val ac = Math.abs(posY - prevPosY).toDouble()
        val cb = Math.abs(posX - prevPosX).toDouble()
        return String.format("%.3f", Math.hypot(ac, cb))
    }

    private fun getInputCharType(str: String): String {
        return if (str.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*".toRegex()))
            "H"
        else if (str.matches("^[a-zA-Z]*$".toRegex()))
            "E"
        else if (str.matches("^[0-9]*$".toRegex()))
            "N"
        else
            "S"
    }

    private fun printHashMap(map: HashMap<Int, KeyObject>) {
        Log.e("AA", "map size: " + map.size)
        for ((key, value) in map) {
            Log.e("AA", key.toString() + " : " + value.appName + ", " + value.inputChar + ", " +
                    value.posX + ",  " + value.posY)
        }
    }


    fun getAppNameByPackageName(context: Context, packageName: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null.toString()
        }

    }


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

    override fun onStart() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun checkAvailability(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val requiredPermissions: List<String>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val newIntentForSetUp: Intent?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val descriptionRes: Int?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val nameRes: Int?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun handleActivityResult(resultCode: Int, intent: Intent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}