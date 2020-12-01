package kaist.iclab.abclogger.collector.keylog

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kaist.iclab.abclogger.ui.settings.keylog.KeyLogSettingActivity
import kaist.iclab.abclogger.commons.*
import kaist.iclab.abclogger.core.DataRepository
import kaist.iclab.abclogger.core.collector.*
import org.koin.android.ext.android.inject
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.hypot

class KeyLogCollector(
    context: Context,
    qualifiedName: String,
    name: String,
    description: String,
    dataRepository: DataRepository
) : AbstractCollector<KeyLogEntity>(
    context,
    qualifiedName,
    name,
    description,
    dataRepository
) {
    var keyboardType by ReadWriteStatusInt(-1)

    private val keyLog: AtomicReference<KeyLog> = AtomicReference()

    override val permissions: List<String> = listOf()

    override val setupIntent: Intent? = Intent(context, KeyLogSettingActivity::class.java)

    override fun isAvailable(): Boolean =
        isAccessibilityServiceRunning<CollectorService>(context) && keyboardType >= 0

    override fun getDescription(): Array<Description> = arrayOf()

    override suspend fun onStart() {
        keyLog.set(null)
    }

    override suspend fun onStop() {}

    override suspend fun count(): Long = dataRepository.count<KeyLogEntity>()

    override suspend fun flush(entities: Collection<KeyLogEntity>) {
        dataRepository.remove(entities)
        recordsUploaded += entities.size
    }

    override suspend fun list(limit: Long): Collection<KeyLogEntity> = dataRepository.find(0, limit)

    fun isAccessibilityServiceRunning() = isAccessibilityServiceRunning<CollectorService>(context)

    data class KeyLog(
        val eventTime: Long = 0,
        val text: String = "",
        val decomposedText: String = "",
        val type: KeyType = KeyType.UNKNOWN,
        val key: String = ""
    )

    enum class KeyType {
        UNKNOWN,
        KOR,
        ENG,
        NUMBER,
        SPECIAL;
    }

    class CollectorService : AccessibilityService() {
        private val collector: KeyLogCollector by inject()

        override fun onInterrupt() {}

        override fun onAccessibilityEvent(accessibilityEvent: AccessibilityEvent) {
            if (collector.getStatus() != Status.On) return

            val isChunjiin = collector.keyboardType == KEYBOARD_TYPE_CHUNJIIN
            val packageName = accessibilityEvent.packageName?.toString() ?: return
            val source = accessibilityEvent.source ?: return
            val timestamp = System.currentTimeMillis()
            val eventTime = accessibilityEvent.eventTime
            val eventType = accessibilityEvent.eventType

            handleAccessibilityEvent(
                packageName = packageName,
                source = source,
                timestamp = timestamp,
                eventTime = eventTime,
                eventType = eventType,
                isChunjiin = isChunjiin
            )
        }

        private fun hasMask(input: Int, mask: Int) = input and mask == mask

        private fun handleAccessibilityEvent(
            packageName: String,
            source: AccessibilityNodeInfo,
            timestamp: Long,
            eventTime: Long,
            eventType: Int,
            isChunjiin: Boolean
        ) = collector.launch {
            if (hasMask(eventType, AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED)) {
                trackNewInput(
                    node = source,
                    packageName = packageName,
                    timestamp = timestamp,
                    eventTime = eventTime,
                    isChunjiin = isChunjiin
                )
            }

            if (hasMask(eventType, AccessibilityEvent.TYPE_VIEW_FOCUSED)) {
                collector.keyLog.set(
                    extractKeyLog(
                        nodeText = source.text?.toString() ?: "",
                        eventTime = eventTime,
                        prevKeyLog = null
                    )
                )
            }
        }

        /* 새로 입력한 키 정보(키 타입, 거리 등)를 분석하기 위해 입력 진행 중인 EditText 트래킹 */
        private suspend fun trackNewInput(
            packageName: String,
            node: AccessibilityNodeInfo,
            timestamp: Long,
            eventTime: Long,
            isChunjiin: Boolean
        ) {
            val text = node.text?.toString() ?: ""
            val className = node.className?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
            if (className.contains("edittext") || className.contains("autocompletetextview")) {
                val newKeyLog = extractKeyLog(
                    nodeText = text,
                    eventTime = eventTime,
                    prevKeyLog = collector.keyLog.get()
                )
                val oldKeyLog = collector.keyLog.getAndSet(newKeyLog) ?: return
                val distance = calculateDistance(
                    fromKey = oldKeyLog.key,
                    fromKeyType = oldKeyLog.type,
                    toKey = newKeyLog.key,
                    toKeyType = newKeyLog.type,
                    isChunjiin = isChunjiin
                )
                val entity = KeyLogEntity(
                    name = getApplicationName(
                        packageManager = packageManager,
                        packageName = packageName
                    ) ?: "",
                    packageName = packageName,
                    isSystemApp = isSystemApp(
                        packageManager = packageManager,
                        packageName = packageName
                    ),
                    isUpdatedSystemApp = isUpdatedSystemApp(
                        packageManager = packageManager,
                        packageName = packageName
                    ),
                    distance = distance,
                    timeTaken = newKeyLog.eventTime - oldKeyLog.eventTime,
                    keyboardType = stringifyKeyboardType(collector.keyboardType),
                    prevKeyType = oldKeyLog.type.name,
                    /*
                    prevKey = oldKeyLog.key,
                    currentKey = newKeyLog.key,
                     */ // delete due to privacy concerns.
                    currentKeyType = newKeyLog.type.name
                )
                collector.put(
                    entity.apply {
                        this.timestamp = timestamp
                    }
                )
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    trackNewInput(
                        packageName = packageName,
                        node = child,
                        eventTime = eventTime,
                        timestamp = timestamp,
                        isChunjiin = isChunjiin
                    )
                }
            }
        }

        private fun extractKeyLog(nodeText: String, eventTime: Long, prevKeyLog: KeyLog?): KeyLog {
            val curDecomposedText = decomposeText(nodeText)
            val curKey = curDecomposedText.lastOrNull()?.toString() ?: ""
            val prevDecomposedText = prevKeyLog?.decomposedText ?: ""

            /**
             * If the length of current decomposed text is less than the one of previous decomposed text,
             * it means characters are removed by pressing back-space - KeyType.SPECIAL
             *
             * Otherwise, we should get key type from current texts.
             * When previous key log does not exists (so, previous text is empty) then
             */
            val curKeyType = if (curDecomposedText.length < prevDecomposedText.length) {
                KeyType.SPECIAL
            } else {
                getKeyType(curKey)
            }

            return KeyLog(
                eventTime = eventTime,
                text = nodeText,
                decomposedText = curDecomposedText,
                key = curKey,
                type = curKeyType
            )
        }

        private fun calculateDistance(
            fromKey: String,
            fromKeyType: KeyType,
            toKey: String,
            toKeyType: KeyType,
            isChunjiin: Boolean
        ): Float {
            if (fromKey.isEmpty()) return Float.MIN_VALUE
            if (toKey.isEmpty()) return Float.MIN_VALUE
            if (fromKeyType != toKeyType) return Float.MIN_VALUE
            if (toKeyType !in arrayOf(KeyType.ENG, KeyType.KOR)) return Float.MIN_VALUE

            val (fromX, fromY) = findPosition(
                key = fromKey,
                keyType = fromKeyType,
                isChunjiin = isChunjiin
            ) ?: return Float.MIN_VALUE
            val (toX, toY) = findPosition(
                key = toKey,
                keyType = toKeyType,
                isChunjiin = isChunjiin
            ) ?: return Float.MIN_VALUE

            val distX = abs(fromX - toX)
            val distY = abs(fromY - toY)

            return hypot(distY, distX)
        }

        private fun findPosition(
            key: String,
            keyType: KeyType,
            isChunjiin: Boolean
        ): Pair<Float, Float>? = when {
            keyType == KeyType.KOR && isChunjiin -> CHUNJIIN
            keyType == KeyType.KOR && !isChunjiin -> QWERTY_KOR
            keyType == KeyType.ENG -> QWERTY_ENG
            else -> null
        }?.get(key)

        private fun getKeyType(str: String?): KeyType = when {
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

        private fun stringifyKeyboardType(flag: Int) = when (flag) {
            KEYBOARD_TYPE_CHUNJIIN -> "CHUNJIIN"
            KEYBOARD_TYPE_QWERTY_KOR -> "QWERTY_KOR"
            KEYBOARD_TYPE_OTHERS -> "OTHERS"
            else -> "UNKNOWN"
        }
    }

    companion object {
        const val KEYBOARD_TYPE_CHUNJIIN = 0x01
        const val KEYBOARD_TYPE_QWERTY_KOR = 0x02
        const val KEYBOARD_TYPE_OTHERS = 0x00

        /**
         * Coordinates for CHUNJIIN
         */
        private val KEYS_CHUNJIIN = arrayOf(
            "l",
            "ㆍ",
            "ㅡ",
            "backspace",
            "ㄱ",
            "ㅋ",
            "ㄴ",
            "ㄹ",
            "ㄷ",
            "ㅌ",
            "ㅂ",
            "ㅍ",
            "ㅅ",
            "ㅎ",
            "ㅈ",
            "ㅊ",
            ".",
            "?",
            "!",
            "ㅇ",
            "ㅁ",
            " ",
            "@",
            "ㅏ",
            "ㅑ",
            "ㅓ",
            "ㅕ",
            "ㅗ",
            "ㅛ",
            "ㅜ",
            "ㅠ",
            "ㅡ",
            "ㅣ",
            "ㅐ",
            "ㅔ",
            "ㅒ",
            "ㅖ",
            "ㅙ",
            "ㅞ",
            "ㅘ",
            "ㅝ",
            "ㅚ",
            "ㅟ",
            "ㅢ"
        )
        private val COORDINATE_X_CHUNJIIN = arrayOf(
            1f,
            2f,
            3f,
            4f,
            1f,
            1f,
            2f,
            2f,
            3f,
            3f,
            1f,
            1f,
            2f,
            2f,
            3f,
            3f,
            4f,
            4f,
            4f,
            2f,
            2f,
            3f,
            4f,
            2f,
            2f,
            1f,
            1f,
            3f,
            3f,
            2f,
            2f,
            3f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            2f,
            1f,
            1f,
            1f,
            1f
        )
        private val COORDINATE_Y_CHUNJIIN = arrayOf(
            1f,
            1f,
            1f,
            1f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            4f,
            4f,
            4f,
            4f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f
        )
        private val CHUNJIIN = KEYS_CHUNJIIN.mapIndexed { index, key ->
            Triple(key, COORDINATE_X_CHUNJIIN[index], COORDINATE_Y_CHUNJIIN[index])
        }.associate { (key, x, y) ->
            key to (x to y)
        }

        private val KEYS_QWERTY_KOR = arrayOf(
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "0",
            "ㅂ",
            "ㅈ",
            "ㄷ",
            "ㄱ",
            "ㅅ",
            "ㅛ",
            "ㅕ",
            "ㅑ",
            "ㅐ",
            "ㅔ",
            "ㅃ",
            "ㅉ",
            "ㄸ",
            "ㄲ",
            "ㅆ",
            "ㅒ",
            "ㅖ",
            "ㅁ",
            "ㄴ",
            "ㅇ",
            "ㄹ",
            "ㅎ",
            "ㅗ",
            "ㅓ",
            "ㅏ",
            "ㅣ",
            "ㅋ",
            "ㅌ",
            "ㅊ",
            "ㅍ",
            "ㅠ",
            "ㅜ",
            "ㅡ",
            "backspace",
            "@",
            " ",
            "."
        )
        private val COORDINATE_X_QWERTY_KOR = arrayOf(
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            1f,
            2f,
            3f,
            4f,
            5f,
            8f,
            9f,
            1.5f,
            2.5f,
            3.5f,
            4.5f,
            5.5f,
            6.5f,
            7.5f,
            8.5f,
            9.5f,
            2.5f,
            3.5f,
            4.5f,
            5.5f,
            6.5f,
            7.5f,
            8.5f,
            9.5f,
            3f,
            6f,
            8.5f
        )
        private val COORDINATE_Y_QWERTY_KOR = arrayOf(
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            5f,
            5f,
            5f
        )
        private val QWERTY_KOR = KEYS_QWERTY_KOR.mapIndexed { index, key ->
            Triple(key, COORDINATE_X_QWERTY_KOR[index], COORDINATE_Y_QWERTY_KOR[index])
        }.associate { (key, x, y) ->
            key to (x to y)
        }

        private val KEYS_QWERTY_ENG = arrayOf(
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "0",
            "q",
            "w",
            "e",
            "r",
            "t",
            "y",
            "u",
            "i",
            "o",
            "p",
            "a",
            "s",
            "d",
            "f",
            "g",
            "h",
            "j",
            "k",
            "l",
            "z",
            "x",
            "c",
            "v",
            "b",
            "n",
            "m",
            "backspace",
            "@",
            " ",
            "."
        )
        private val COORDINATE_X_QWERTY_ENG = arrayOf(
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            1f,
            2f,
            3f,
            4f,
            5f,
            6f,
            7f,
            8f,
            9f,
            10f,
            1.5f,
            2.5f,
            3.5f,
            4.5f,
            5.5f,
            6.5f,
            7.5f,
            8.5f,
            9.5f,
            2.5f,
            3.5f,
            4.5f,
            5.5f,
            6.5f,
            7.5f,
            8.5f,
            9.5f,
            3f,
            6f,
            8.5f
        )
        private val COORDINATE_Y_QWERTY_ENG = arrayOf(
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            2f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            3f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            4f,
            5f,
            5f,
            5f
        )
        private val QWERTY_ENG = KEYS_QWERTY_ENG.mapIndexed { index, key ->
            Triple(key, COORDINATE_X_QWERTY_ENG[index], COORDINATE_Y_QWERTY_ENG[index])
        }.associate { (key, x, y) ->
            key to (x to y)
        }

        /*한글 초성,중성,종성 별 ASCII 코드*/
        private val CHOSUNG = arrayOf(
            0x3131.toChar(),
            0x3132.toChar(),
            0x3134.toChar(),
            0x3137.toChar(),
            0x3138.toChar(),
            0x3139.toChar(),
            0x3141.toChar(),
            0x3142.toChar(),
            0x3143.toChar(),
            0x3145.toChar(),
            0x3146.toChar(),
            0x3147.toChar(),
            0x3148.toChar(),
            0x3149.toChar(),
            0x314a.toChar(),
            0x314b.toChar(),
            0x314c.toChar(),
            0x314d.toChar(),
            0x314e.toChar()
        )
        private val JOONGSUNG = arrayOf(
            0x314f.toChar(),
            0x3150.toChar(),
            0x3151.toChar(),
            0x3152.toChar(),
            0x3153.toChar(),
            0x3154.toChar(),
            0x3155.toChar(),
            0x3156.toChar(),
            0x3157.toChar(),
            0x3158.toChar(),
            0x3159.toChar(),
            0x315a.toChar(),
            0x315b.toChar(),
            0x315c.toChar(),
            0x315d.toChar(),
            0x315e.toChar(),
            0x315f.toChar(),
            0x3160.toChar(),
            0x3161.toChar(),
            0x3162.toChar(),
            0x3163.toChar()
        )
        private val JONGSUNG = arrayOf(
            0.toChar(),
            0x3131.toChar(),
            0x3132.toChar(),
            0x3133.toChar(),
            0x3134.toChar(),
            0x3135.toChar(),
            0x3136.toChar(),
            0x3137.toChar(),
            0x3139.toChar(),
            0x313a.toChar(),
            0x313b.toChar(),
            0x313c.toChar(),
            0x313d.toChar(),
            0x313e.toChar(),
            0x313f.toChar(),
            0x3140.toChar(),
            0x3141.toChar(),
            0x3142.toChar(),
            0x3144.toChar(),
            0x3145.toChar(),
            0x3146.toChar(),
            0x3147.toChar(),
            0x3148.toChar(),
            0x314a.toChar(),
            0x314b.toChar(),
            0x314c.toChar(),
            0x314d.toChar(),
            0x314e.toChar()
        )

    }
}