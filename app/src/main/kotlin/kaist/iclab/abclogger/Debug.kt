package kaist.iclab.abclogger

import android.util.Log
import kaist.iclab.abclogger.collector.activity.PhysicalActivityTransitionEntity
import kaist.iclab.abclogger.collector.fill
import kaist.iclab.abclogger.collector.survey.SurveyEntity
import java.util.concurrent.TimeUnit

object Debug {

    /*fun generateEntities(context: Context, size: Int) {
        TestUtils.generateBatteryEntities(size)
        TestUtils.generateCallLogEntities(size)
        TestUtils.generateConnectivityEntities(size)
        TestUtils.generateDataTrafficEntities(size)
        TestUtils.generateDeviceEventEntities(size)
        TestUtils.generateEmotionalStatusEntities(size)
        TestUtils.generateExperienceSampleEntities(size)
        TestUtils.generateInteractionEntities(size)
        TestUtils.generateMediaEntities(size)
        TestUtils.generateLocationEntities(size)
        TestUtils.generateNotificationEntities(size)
        TestUtils.generatePhysicalActivityEntities(size)
        TestUtils.generateRecordEntities(size, context)
        TestUtils.generateSensorEntities(size)
        TestUtils.generateWeatherEntities(size)
        TestUtils.generateWifiEntities(size)
        TestUtils.generateMessageEntities(size)
    }

     fun generateBatteryEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<BatteryEntity>().put(List(size) {
        BatteryEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateCallLogEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<CallLogEntity>().put(List(size) {
        CallLogEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateConnectivityEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<ConnectivityEntity>().put(List(size) {
        ConnectivityEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateDataTrafficEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<DataTrafficEntity>().put(List(size) {
        DataTrafficEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateDeviceEventEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<DeviceEventEntity>().put(List(size) {
        DeviceEventEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateEmotionalStatusEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<EmotionalStatusEntity>().put(List(size) {
        EmotionalStatusEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateExperienceSampleEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<SurveyEntity>().put(List(size) {
        SurveyEntity(            deliveredTime = System.currentTimeMillis() - it * 1000 * 60 * 60
        ).apply {
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateInteractionEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<AppUsageEventEntity>().put(List(size) {
        AppUsageEventEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateLocationEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<LocationEntity>().put(List(size) {
        LocationEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateMediaEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<MediaEntity>().put(List(size) {
        MediaEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateNotificationEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<NotificationEntity>().put(List(size) {
        NotificationEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generatePhysicalActivityEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<PhysicalActivityEventEntity>().put(List(size) {
        PhysicalActivityEventEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateRecordEntities (size: Int, context: Context) = App.getBoxStore(applicationContext).boxFor<RecordEntity>().put(List(size) {
         val fileName = (System.currentTimeMillis() - it * 1000 * 60 * 60).toString()
         val fileSize = 1000 * 1000 * 1
         val file = File(context.getExternalFilesDir(null), fileName)
         FileOutputStream(file).use { stream ->
             val buffer = ByteArray(1000) {
                it.toByte()
            }
            var pos = 0
            while(pos < fileSize) {
                pos += buffer.size
                stream.write(buffer)
            }
        }
        RecordEntity(
            path = file.absolutePath
        ).apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateSensorEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<SensorEntity>().put(List(size) {
        SensorEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateWeatherEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<WeatherEntity>().put(List(size) {
        WeatherEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

     fun generateWifiEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<WifiEntity>().put(List(size) {
        WifiEntity(
            bssid = "asdfasdfasfdzvzxcv",
            ssid = "111111"
        ).apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })

    fun generateMessageEntities (size: Int) = App.getBoxStore(applicationContext).boxFor<MessageEntity>().put(List(size) {
        MessageEntity().apply {
            timestamp = System.currentTimeMillis() - it * 1000 * 60 * 60
            utcOffset = FormatUtils.utcOffsetInHour()
        }
    })*/

    fun generateEntities(size: Int) {
        ObjBox.boxFor<PhysicalActivityTransitionEntity>()?.removeAll()
        (0..size).map {
            PhysicalActivityTransitionEntity(type = "ASDFASDFASDFASDFASDFASDFASDF", isEntered = true).fill(System.currentTimeMillis())
        }.let { ObjBox.put(it) }
        Log.d("ZXCV", "${ObjBox.boxFor<PhysicalActivityTransitionEntity>()?.count()}")
    }

    fun generateSurveyEntities (size: Int) {
        ObjBox.boxFor<SurveyEntity>()?.removeAll()
        val time = System.currentTimeMillis()
        (0..size).map {
            SurveyEntity(
                    title = "$it",
                    message = "설문에 응답해주세요",
                    timeoutSec = 600,
                    timeoutPolicy = "DISABLED",
                    deliveredTime = time + it * TimeUnit.MINUTES.toMillis(5),
                    json = testJson
            ).fill(System.currentTimeMillis())
        }.let {
            ObjBox.put(it)
        }

    }

    private val testJson = """
        {
          "type": "INTERVAL",
          "title": "감정 설문이 도착했습니다",
          "message": "설문에 응답해주세요",
          "instruction": "알림 도착 이후 10분 이내의 상황을 기준으로 아래에 응답해주세요",
          "timeoutSec": 600,
          "timeoutPolicy": "DISABLED",
          "initialDelaySec": 0,
          "intervalSec": 2400,
          "flexIntervalSec": 1200,
          "dailyStartTimeHour": 0,
          "dailyStartTimeMinute": 0,
          "dailyEndTimeHour": 24,
          "dailyEndTimeMinute": 0,
          "questions": [
            {
              "type": "RADIO_BUTTON",
              "shouldAnswer": true,
              "showEtc": true,
              "text": "My emotion right before doing this survey could be rated as (-3: very negative ~ +3: very positive)",
              "options": ["-3", "-2", "-1", "0", "1", "2", "3"]
            },
            {
              "type": "RADIO_BUTTON",
              "shouldAnswer": true,
              "showEtc": false,
              "text": "My emotion right before doing this survey could be rated as (-3: very calm ~ +3: very excited)",
              "options": ["-3", "-2", "-1", "0", "1", "2", "3"]
            },
            {
              "type": "CHECK_BOX",
              "shouldAnswer": true,
              "showEtc": true,
              "text": "My stress level right before doing this survey could be rated as (-3: not stressed at all ~ +3: very stressed)",
              "options": ["-3", "-2", "-1", "0", "1", "2", "3"]
            },
            {
              "type": "CHECK_BOX",
              "shouldAnswer": true,
              "showEtc": false,
              "text": "My attention level right before doing this survey could be rated as (-3: very bored ~ +3: very engaged)",
              "options": ["-3", "-2", "-1", "0", "1", "2", "3"]
            },
            {
              "type": "SLIDER",
              "shouldAnswer": true,
              "showEtc": false,
              "text": "The emotion that I answered with above has not changed for the past ___ minutes. (0 ~ 60 min; or type \"I'm not sure.\" if you are not sure)",
              "options": ["50", "60"]
            },
            {
              "type": "SLIDER",
              "shouldAnswer": true,
              "showEtc": true,
              "text": "The emotion that I answered with above has not changed for the past ___ minutes. (0 ~ 60 min; or type \"I'm not sure.\" if you are not sure)",
              "options": ["50", "60"]
            },

            {
              "type": "FREE_TEXT",
              "shouldAnswer": true,
              "showEtc": false,
              "text": "Did answering this survey disturb the activity that I was performing before doing it? (-3: did not disturb me at all ~ +3: it was very disturbing)"
            },
            {
              "type": "FREE_TEXT",
              "shouldAnswer": true,
              "showEtc": true,
              "text": "How mentally demanding was my activity before doing this survey? (-3: very low ~ +3: very high)"
            }

          ]
        }
    """.trimIndent()
}