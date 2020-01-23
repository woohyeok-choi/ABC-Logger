package kaist.iclab.abclogger.collector.keylog

import android.accessibilityservice.AccessibilityService
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kaist.iclab.abclogger.*
import kaist.iclab.abclogger.base.BaseCollector
import kaist.iclab.abclogger.collector.getApplicationName
import kaist.iclab.abclogger.collector.isSystemApp
import kaist.iclab.abclogger.collector.isUpdatedSystemApp
import java.util.*
import kotlin.math.abs
import kotlin.math.hypot

class KeyLogCollector(val context: Context) : BaseCollector {

    data class KeyLog(
            val timestamp: Long = 0,
            val text: String = "",
            val decomposedText: String = "",
            val type: KeyType = KeyType.SPECIAL,
            val key: String = "")

    enum class KeyType {
        KOR,
        ENG,
        NUMBER,
        SPECIAL;
    }

    class KeyLogCollectorService: AccessibilityService() {
        private var keyLog = KeyLog()

        private fun hasMask(input: Int, mask: Int) = input and mask == mask

        /* 새로 입력한 키 정보(키 타입, 거리 등)를 분석하기 위해 입력 진행 중인 EditText 트래킹 */
        private fun trackNewInput(node: AccessibilityNodeInfo,
                                  packageName: String,
                                  eventTime: Long,
                                  isChunjiin: Boolean) {
            if(node.text?.isNotEmpty() == true && "edittext" in node.className.toString().toLowerCase(Locale.getDefault())) {
                val newKeyLog = handleTextChanged(
                        node = node, eventTime = eventTime, prevKeyLog = keyLog
                )
                val distance = calculateDistance(
                        fromKey = keyLog.key, fromKeyType = keyLog.type,
                        toKey = newKeyLog.key, toKeyType = newKeyLog.type,
                        isChunjiin = isChunjiin
                )

                KeyLogEntity(
                        name = getApplicationName(packageManager = packageManager, packageName = packageName)
                                ?: "",
                        packageName = packageName,
                        isSystemApp = isSystemApp(packageManager = packageManager, packageName = packageName),
                        isUpdatedSystemApp = isUpdatedSystemApp(packageManager = packageManager, packageName = packageName),
                        distance = distance,
                        timeTaken = newKeyLog.timestamp - keyLog.timestamp,
                        keyboardType = CollectorPrefs.softKeyboardType,
                        prevKey = keyLog.key,
                        prevKeyType = keyLog.type.name,
                        currentKey = newKeyLog.key,
                        currentKeyType = newKeyLog.type.name
                ).fill(timeMillis = eventTime).run { ObjBox.put(this) }

                keyLog = newKeyLog
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) trackNewInput(node = child, packageName = packageName, eventTime = eventTime, isChunjiin = isChunjiin) else return
            }
        }

        private fun handleFocused(node: AccessibilityNodeInfo, eventTime: Long) : KeyLog {
            val text = node.text?.toString() ?: ""
            val decomposedText = decomposeText(text)
            val key = text.lastOrNull()?.toString() ?: ""
            val type = getKeyType(key)


            return KeyLog(
                    timestamp = eventTime,
                    text = text,
                    decomposedText = decomposedText,
                    key = key,
                    type = type
            )
        }

        private fun handleTextChanged(node: AccessibilityNodeInfo, eventTime: Long, prevKeyLog: KeyLog) : KeyLog {
            if (node.text.isNullOrEmpty()) return KeyLog(timestamp = eventTime)

            val text = node.text.toString()
            val decomposedText = decomposeText(text)
            val key = decomposedText.lastOrNull()?.toString() ?: ""
            val keyType = if (decomposedText.length >= prevKeyLog.decomposedText.length && key.isNotEmpty()) {
                getKeyType(key)
            } else {
                KeyType.SPECIAL
            }

            return KeyLog(
                    timestamp = eventTime,
                    text = text,
                    decomposedText = decomposedText,
                    key = key,
                    type = keyType
            )
        }

        private fun calculateDistance(fromKey: String, fromKeyType: KeyType, toKey: String, toKeyType: KeyType, isChunjiin: Boolean) : Float {
            if (fromKeyType != toKeyType || toKeyType in arrayOf(KeyType.SPECIAL, KeyType.NUMBER) || fromKey.isEmpty() || toKey.isEmpty()) return 0.0F

            val (fromX, fromY) = findPosition(key = fromKey, keyType = fromKeyType, isChunjiin = isChunjiin) ?: return 0.0F
            val (toX, toY) = findPosition(key = toKey, keyType = toKeyType, isChunjiin = isChunjiin) ?: return 0.0F

            val distX = abs(fromX - toX)
            val distY = abs(fromY - toY)

            return hypot(distY, distX)
        }


        private fun findPosition(key: String, keyType: KeyType, isChunjiin: Boolean) : Pair<Float, Float>? {
            val setting = when {
                keyType == KeyType.KOR && isChunjiin -> CHUNJIIN
                keyType == KeyType.KOR && !isChunjiin -> QWERTY_KOR
                keyType == KeyType.ENG -> QWERTY_ENG
                else -> listOf()
            }

            return setting.find { (k, _, _) -> k == key }?.let { (_, x, y) -> x to y }
        }


        /* 현재 입력한 키의 타입(예. 한글, 영어 등)을 반환하는 함수 */
        private fun getKeyType(str: String?): KeyType =
                when {
                    str?.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*".toRegex()) == true -> KeyType.KOR    // 한글
                    str?.matches("^[a-zA-Z]*$".toRegex()) == true -> KeyType.ENG// 영어
                    str?.matches("^[0-9]*$".toRegex()) == true -> KeyType.NUMBER    // 숫자
                    else -> KeyType.SPECIAL// 특수문자
                }

        private fun decomposeText(text: String): String {
            var a: Int
            var b: Int
            var c: Int // 자소 버퍼: 초성/중성/종성 순
            var result = ""

            for (element in text) {
                if (element.toInt() in 0xAC00..0xD7A3) { // "AC00:가" ~ "D7A3:힣" 에 속한 글자면 분해
                    c = element.toInt() - 0xAC00
                    a = c / (21 * 28)
                    c %= (21 * 28)
                    b = c / 28
                    c %= 28

                    result = result + CHOSUNG[a] + JOONGSUNG[b]
                    if (c != 0) result += JONGSUNG[c] // c가 0이 아니면, 즉 받침이 있으면
                } else {
                    result += element
                }
            }
            return result
        }

        override fun onInterrupt() {}

        override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
            if(!CollectorPrefs.hasStartedKeyStrokes) return
            accessibilityEvent.packageName ?: return
            accessibilityEvent.source ?: return

            val eventType = accessibilityEvent.eventType

            if (hasMask(eventType, AccessibilityEvent.TYPE_VIEW_FOCUSED)) {
                trackNewInput(
                        node = accessibilityEvent.source,
                        packageName = accessibilityEvent.packageName.toString(),
                        eventTime = accessibilityEvent.eventTime,
                        isChunjiin = CollectorPrefs.softKeyboardType == KEYBOARD_TYPE_CHUNJIIN
                )
            }

            if (hasMask(eventType, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)) {
                keyLog = handleFocused(node = accessibilityEvent.source, eventTime = accessibilityEvent.eventTime)
            }
        }
    }

    override suspend fun onStart() { }

    override suspend fun onStop() { }

    override fun checkAvailability(): Boolean {
        val serviceName = "${context.packageName}/${KeyLogCollectorService::class.java.canonicalName}"
        val isEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED, 0
        ) == 1
        val isIncluded = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.replace("$", ".")?.split(":")?.contains(serviceName) ?: false

        return isEnabled && isIncluded && CollectorPrefs.softKeyboardType.isNotEmpty()
    }

    override val requiredPermissions: List<String>
        get() = listOf()

    override val newIntentForSetUp: Intent? = Intent(context, KeyLogSettingActivity::class.java)

    companion object {
        const val KEYBOARD_TYPE_CHUNJIIN = "KEYBOARD_TYPE_CHUNJIIN"
        const val KEYBOARD_TYPE_QWERTY_KOR = "KEYBOARD_TYPE_QWERTY_KOR"
        const val KEYBOARD_TYPE_UNKNOWN = "KEYBOARD_TYPE_UNKNOWN"

        /* 각 키보드 유형별 키 좌표 */
        private val KEYS_CHUNJIIN = arrayOf("l", "ㆍ", "ㅡ", "backspace", "ㄱ", "ㅋ", "ㄴ", "ㄹ", "ㄷ", "ㅌ", "ㅂ", "ㅍ", "ㅅ", "ㅎ", "ㅈ", "ㅊ", ".", "?", "!", "ㅇ", "ㅁ", " ", "@", "ㅏ", "ㅑ", "ㅓ", "ㅕ", "ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "ㅣ", "ㅐ", "ㅔ", "ㅒ", "ㅖ", "ㅙ", "ㅞ", "ㅘ", "ㅝ", "ㅚ", "ㅟ", "ㅢ")
        private val COORDINATE_X_CHUNJIIN = arrayOf(1f, 2f, 3f, 4f, 1f, 1f, 2f, 2f, 3f, 3f, 1f, 1f, 2f, 2f, 3f, 3f, 4f, 4f, 4f, 2f, 2f, 3f, 4f, 2f, 2f, 1f, 1f, 3f, 3f, 2f, 2f, 3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f)
        private val COORDINATE_Y_CHUNJIIN = arrayOf(1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        private val CHUNJIIN = KEYS_CHUNJIIN.mapIndexed { index, key -> Triple(key, COORDINATE_X_CHUNJIIN[index], COORDINATE_Y_CHUNJIIN[index]) }

        private val KEYS_QWERTY_KOR = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "ㅂ", "ㅈ", "ㄷ", "ㄱ", "ㅅ", "ㅛ", "ㅕ", "ㅑ", "ㅐ", "ㅔ", "ㅃ", "ㅉ", "ㄸ", "ㄲ", "ㅆ", "ㅒ", "ㅖ", "ㅁ", "ㄴ", "ㅇ", "ㄹ", "ㅎ", "ㅗ", "ㅓ", "ㅏ", "ㅣ", "ㅋ", "ㅌ", "ㅊ", "ㅍ", "ㅠ", "ㅜ", "ㅡ", "backspace", "@", " ", ".")
        private val COORDINATE_X_QWERTY_KOR = arrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 8f, 9f, 1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 3f, 6f, 8.5f)
        private val COORDINATE_Y_QWERTY_KOR = arrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 5f, 5f, 5f)
        private val QWERTY_KOR = KEYS_QWERTY_KOR.mapIndexed { index, key -> Triple(key, COORDINATE_X_QWERTY_KOR[index], COORDINATE_Y_QWERTY_KOR[index]) }

        private val KEYS_QWERTY_ENG = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "a", "s", "d", "f", "g", "h", "j", "k", "l", "z", "x", "c", "v", "b", "n", "m", "backspace", "@", " ", ".")
        private val COORDINATE_X_QWERTY_ENG = arrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 1.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 2.5f, 3.5f, 4.5f, 5.5f, 6.5f, 7.5f, 8.5f, 9.5f, 3f, 6f, 8.5f)
        private val COORDINATE_Y_QWERTY_ENG = arrayOf(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 3f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 4f, 5f, 5f, 5f)
        private val QWERTY_ENG = KEYS_QWERTY_ENG.mapIndexed { index, key -> Triple(key, COORDINATE_X_QWERTY_ENG[index], COORDINATE_Y_QWERTY_ENG[index]) }

        /*한글 초성,중성,종성 별 ASCII 코드*/
        private val CHOSUNG = arrayOf(0x3131.toChar(), 0x3132.toChar(), 0x3134.toChar(), 0x3137.toChar(), 0x3138.toChar(), 0x3139.toChar(), 0x3141.toChar(), 0x3142.toChar(), 0x3143.toChar(), 0x3145.toChar(), 0x3146.toChar(), 0x3147.toChar(), 0x3148.toChar(), 0x3149.toChar(), 0x314a.toChar(), 0x314b.toChar(), 0x314c.toChar(), 0x314d.toChar(), 0x314e.toChar())
        private val JOONGSUNG = arrayOf(0x314f.toChar(), 0x3150.toChar(), 0x3151.toChar(), 0x3152.toChar(), 0x3153.toChar(), 0x3154.toChar(), 0x3155.toChar(), 0x3156.toChar(), 0x3157.toChar(), 0x3158.toChar(), 0x3159.toChar(), 0x315a.toChar(), 0x315b.toChar(), 0x315c.toChar(), 0x315d.toChar(), 0x315e.toChar(), 0x315f.toChar(), 0x3160.toChar(), 0x3161.toChar(), 0x3162.toChar(), 0x3163.toChar())
        private val JONGSUNG = arrayOf(0.toChar(), 0x3131.toChar(), 0x3132.toChar(), 0x3133.toChar(), 0x3134.toChar(), 0x3135.toChar(), 0x3136.toChar(), 0x3137.toChar(), 0x3139.toChar(), 0x313a.toChar(), 0x313b.toChar(), 0x313c.toChar(), 0x313d.toChar(), 0x313e.toChar(), 0x313f.toChar(), 0x3140.toChar(), 0x3141.toChar(), 0x3142.toChar(), 0x3144.toChar(), 0x3145.toChar(), 0x3146.toChar(), 0x3147.toChar(), 0x3148.toChar(), 0x314a.toChar(), 0x314b.toChar(), 0x314c.toChar(), 0x314d.toChar(), 0x314e.toChar())
    }


}