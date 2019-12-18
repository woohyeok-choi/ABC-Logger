package kaist.iclab.abclogger.common.util

object TestUtils {
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
}