package kaist.iclab.abclogger.background

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kaist.iclab.abclogger.common.type.KeyObject
import kaist.iclab.abclogger.common.util.AnalyzeHangeul
import kaist.iclab.abclogger.data.MySQLiteLogger
import java.util.*

class KeyTrackingService : AccessibilityService() {

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
        val decomposeCurrentStr = AnalyzeHangeul.hangulToJaso(currentStr)
        //Log.w("AccessibilityService", "Current 문자 분해: $decomposeCurrentStr")

        val decomposeTotalStr = AnalyzeHangeul.hangulToJaso(totalStr)
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
                    MySQLiteLogger.writeStringData(applicationContext, "AccessibilityLog", System.currentTimeMillis(), jsonEntity)

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


}