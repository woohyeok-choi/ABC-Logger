package kaist.iclab.abclogger.communication

import io.grpc.ManagedChannel

// import kaist.iclab.abc.protos.*

object GrpcApi {
    private var channel: ManagedChannel? = null
}
/*
object GrpcApi {
    private var channel: ManagedChannel? = null
    private var uploadStub: RawDataManagementApiGrpc.RawDataManagementApiStub? = null
    private var weatherApiStub: WeatherApiGrpc.WeatherApiBlockingStub? = null
    private var experimentStub: ExperimentApiGrpc.ExperimentApiBlockingStub? = null
    private var logStub: LogApiGrpc.LogApiStub? = null

    fun getChannel(): ManagedChannel {
        channel = channel ?:
            ManagedChannelBuilder.forTarget(BuildConfig.SERVER_ADDRESS).usePlaintext().build()
        return channel!!
    }

    private fun getUploadStub() : RawDataManagementApiGrpc.RawDataManagementApiStub {
        uploadStub = uploadStub ?: RawDataManagementApiGrpc.newStub(getChannel())
        return uploadStub!!.withDeadlineAfter(1, TimeUnit.MINUTES)
    }

    private fun getWeatherStub() : WeatherApiGrpc.WeatherApiBlockingStub {
        weatherApiStub = weatherApiStub ?: WeatherApiGrpc.newBlockingStub(getChannel())
        return weatherApiStub!!.withDeadlineAfter(10, TimeUnit.SECONDS)
    }

    private fun getExperimentStub() : ExperimentApiGrpc.ExperimentApiBlockingStub {
        experimentStub = experimentStub ?: ExperimentApiGrpc.newBlockingStub(getChannel())
        return experimentStub!!.withDeadlineAfter(10, TimeUnit.SECONDS)
    }

    private fun getLogStub(): LogApiGrpc.LogApiStub {
        logStub = logStub ?: LogApiGrpc.newStub(getChannel())
        return logStub!!.withDeadlineAfter(2, TimeUnit.MINUTES)
    }

    fun <T: Base> uploadEntities(entities: List<T>) {
        val countDownLatch = CountDownLatch(1)
        var exception: Throwable? = null

        val responseObserver = object : StreamObserver<CommonProtos.Empty> {
            override fun onNext(value: CommonProtos.Empty?) { }

            override fun onError(t: Throwable?) {
                exception = t
                countDownLatch.countDown()
            }

            override fun onCompleted() {
                countDownLatch.countDown()
            }
        }

        val requestObserver = getUploadStub()
            .createRawData(responseObserver)

        try {
            for(entity in entities) {
                entityToRawDataProto(entity)?.run {
                    requestObserver.onNext(this)
                    SystemClock.sleep(50)
                }

                if(countDownLatch.count == 0L) {
                    break
                }
            }
        } catch (e: Exception) {
            requestObserver.onError(e)
        }
        requestObserver.onCompleted()

        if(!countDownLatch.await(1L, TimeUnit.MINUTES)){
            throw ServerUnavailableException()
        }

        if(exception != null) {
            throw RuntimeException(exception)
        }
    }

    private fun <T: Base> entityToRawDataProto(entity: T): ResourceProtos.RawData? {
        val builder = ResourceProtos.RawData.newBuilder()
            .setTime(
                CommonProtos.Time.newBuilder()
                    .setTimestamp(entity.timestamp)
                    .setUtcoffset(entity.utcOffset)
                    .build()
            )
            .setEmail(entity.subjectEmail)
            .setExperimentGroup(entity.experimentGroup)
            .setExperimentUuid(entity.experimentUuid)
            .setExperimentGroup(entity.experimentGroup)
            .setSourceInfo(FormatUtils.formatProductName())
            .setSourceType(EnumProtos.DataSourceType.PHONE)

        return when(entity) {
            is AppUsageEventEntity -> {
                builder.setAppUsageEvent(
                    ResourceProtos.AppUsageEvent.newBuilder()
                        .setName(entity.name)
                        .setPackageName(entity.packageName)
                        .setIsSystemApp(entity.isSystemApp)
                        .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                        .setType(entity.type.name)
                        .build()
                ).build()
            }
            is AppUsageStatEntity -> {
                builder.setAppUsageStat(
                    ResourceProtos.AppUsageStat.newBuilder()
                        .setName(entity.name)
                        .setPackageName(entity.packageName)
                        .setIsSystemApp(entity.isSystemApp)
                        .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                        .setStartTime(entity.startTime)
                        .setEndTime(entity.endTime)
                        .setLastTimeUsed(entity.lastTimeUsed)
                        .setTotalTimeForeground(entity.totalTimeForeground)
                        .build()
                ).build()
            }
            is BatteryEntity -> {
                builder.setBattery(
                        ResourceProtos.Battery.newBuilder()
                            .setLevel(entity.level)
                            .setTemperature(entity.temperature)
                            .setPlugged(entity.plugged.name)
                            .setStatus(entity.status.name)
                            .build()
                    ).build()
            }
            is CallLogEntity -> {
                builder.setCallLog(
                        ResourceProtos.CallLog.newBuilder()
                            .setNumber(entity.number)
                            .setType(entity.type.name)
                            .setDuration(entity.duration)
                            .setPresentation(entity.presentation.name)
                            .setDataUsage(entity.dataUsage)
                            .setContact(entity.contact.name)
                            .setTimesContacted(entity.timesContacted)
                            .setIsStarred(entity.isStarred)
                            .setIsPinned(entity.isPinned)
                            .build()
                    ).build()
            }
            is ConnectivityEntity -> {
                builder.setConnectivity(
                        ResourceProtos.Connectivity.newBuilder()
                            .setIsConnected(entity.isConnected)
                            .setType(entity.type.name)
                            .build()
                    ).build()
            }
            is DataTrafficEntity -> {
                builder.setDataTraffic(
                        ResourceProtos.DataTraffic.newBuilder()
                            .setDuration(entity.duration)
                            .setRxKb(entity.rxKiloBytes)
                            .setTxKb(entity.txKiloBytes)
                            .build()
                    ).build()
            }
            is DeviceEventEntity -> {
                builder.setDeviceEvent(
                        ResourceProtos.DeviceEvent.newBuilder()
                            .setType(entity.type.name)
                            .build()
                    ).build()
            }
            is EmotionalStatusEntity -> {
                builder.setEmotionalStatus(
                        ResourceProtos.EmotionalStatus.newBuilder()
                            .setAnger(entity.anger)
                            .setContempt(entity.contempt)
                            .setDisgust(entity.disgust)
                            .setFear(entity.fear)
                            .setHappiness(entity.happiness)
                            .setNeutral(entity.neutral)
                            .setSadness(entity.sadness)
                            .setSurprise(entity.surprise)
                            .build()
                    ).build()
            }
            is InstalledAppEntity -> {
                builder.setInstalledApp(
                    ResourceProtos.InstalledApp.newBuilder()
                        .setName(entity.name)
                        .setPackageName(entity.packageName)
                        .setIsSystemApp(entity.isSystemApp)
                        .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                        .setFirstInstallTime(entity.firstInstallTime)
                        .setLastUpdatedTime(entity.lastUpdateTime)
                        .build()
                ).build()
            }
            is LocationEntity -> {
                builder.setLocation(
                        ResourceProtos.Location.newBuilder()
                            .setLatitude(entity.latitude)
                            .setLongitude(entity.longitude)
                            .setAltitude(entity.altitude)
                            .setAccuracy(entity.accuracy)
                            .setSpeed(entity.speed)
                            .build()
                    ).build()
            }
            is MediaEntity -> {
                builder.setMedia(
                    ResourceProtos.Media.newBuilder()
                        .setMimeType(entity.mimetype)
                        .setBucketDisplay(entity.bucketDisplay)
                        .build()
                ).build()
            }
            is MessageEntity -> {
                builder.setMessage(
                    ResourceProtos.Message.newBuilder()
                        .setNumber(entity.number)
                        .setMessageClass(entity.messageClass.name)
                        .setMessageBox(entity.messageBox.name)
                        .setContact(entity.contact.name)
                        .setTimesContacted(entity.timesContacted)
                        .setIsStarred(entity.isStarred)
                        .setIsPinned(entity.isPinned)
                        .build()
                ).build()
            }
            is NotificationEntity -> {
                builder.setNotification(
                    ResourceProtos.Notification.newBuilder()
                        .setName(entity.name)
                        .setPackageName(entity.packageName)
                        .setIsSystemApp(entity.isSystemApp)
                        .setIsUpdatedSystemApp(entity.isUpdatedSystemApp)
                        .setTitle(entity.title)
                        .setVisibility(entity.visibility.name)
                        .setCategory(entity.category)
                        .setHasVibration(entity.hasVibration)
                        .setHasSound(entity.hasSound)
                        .setLightColor(entity.lightColor)
                        .build()
                ).build()
            }
            is PhysicalActivityEventEntity -> {
                builder.setPhysicalActivityEvent(
                    ResourceProtos.PhysicalActivityEvent.newBuilder()
                        .setType(entity.type.name)
                        .setConfidence(entity.confidence)
                        .build()
                ).build()
            }
            is PhysicalActivityTransitionEntity -> {
                builder.setActivityTransition(
                    ResourceProtos.PhysicalActivityTransition.newBuilder()
                        .setTransitionType(entity.transitionType.name)
                        .build()
                ).build()
            }
            is PhysicalStatusEntity -> {
                builder.setPhysicalStatus(
                    ResourceProtos.PhysicalStatus.newBuilder()
                        .setActivity(entity.activity)
                        .setType(entity.type)
                        .setStartTime(entity.startTime)
                        .setEndTime(entity.endTime)
                        .setValue(entity.value)
                        .build()
                ).build()
            }
            is RecordEntity -> {
                val file = File(entity.path)
                if(file.exists() && file.length() <= Math.pow(2.toDouble(), 20.toDouble())) {
                    builder.setBinaryFile(
                        ResourceProtos.BinaryFile.newBuilder()
                            .setMetaData(
                                mapOf("channelMask" to entity.channelMask,
                                    "channelEncoding" to entity.encoding,
                                    "sampleRates" to entity.sampleRate,
                                    "duration" to entity.duration,
                                    "endian" to "BigEndian").toString())
                            .setMimeType("audio/wav")
                            .setSize(file.length())
                            .setData(ByteString.readFrom(FileInputStream(file)))
                            .build()
                    ).build()
                } else {
                    null
                }
            }
            is SensorEntity -> {
                builder.setRawSensor(
                    ResourceProtos.RawSensor.newBuilder()
                        .setType(entity.type)
                        .setValue1(entity.firstValue.toDouble())
                        .setValue2(entity.secondValue.toDouble())
                        .setValue3(entity.thirdValue.toDouble())
                        .setValue4(entity.fourthValue.toDouble())
                        .build()
                ).build()
            }
            is SurveyEntity -> {
                builder.setSurvey(
                    ResourceProtos.Survey.newBuilder()
                        .setTitle(entity.title)
                        .setMessage(entity.message)
                        .setDeliveredTimestamp(entity.deliveredTime)
                        .setReactionTimestamp(entity.reactionTime)
                        .setResponse(entity.responses)
                        .setFirstQuestionTimestamp(entity.firstQuestionTime)
                        .build()
                ).build()
            }
            is WeatherEntity -> {
                builder.setWeather(
                    ResourceProtos.Weather.newBuilder()
                        .setLatitude(entity.latitude)
                        .setLongitude(entity.longitude)
                        .setTemperature(entity.temperature)
                        .setRainfall(entity.rainfall)
                        .setSky(entity.sky)
                        .setWindEw(entity.windEw)
                        .setWindNs(entity.windNs)
                        .setHumidity(entity.humidity)
                        .setRainType(entity.rainType)
                        .setLightning(entity.lightning)
                        .setWindSpeed(entity.windSpeed)
                        .setWindDirection(entity.windDirection)
                        .setSo2Value(entity.so2Value)
                        .setSo2Grade(entity.so2Grade)
                        .setCoValue(entity.coValue)
                        .setCoGrade(entity.coGrade)
                        .setNo2Value(entity.no2Value)
                        .setNo2Grade(entity.no2Grade)
                        .setO3Value(entity.o3Value)
                        .setO3Grade(entity.o3Grade)
                        .setPm10Value(entity.pm10Value)
                        .setPm10Grade(entity.pm10Grade)
                        .setPm25Value(entity.pm25Value)
                        .setPm25Grade(entity.pm25Grade)
                        .setAirValue(entity.airValue)
                        .setAirGrade(entity.airGrade)
                        .build()
                ).build()
            }
            is WifiEntity -> {
                builder.setWifi(
                        ResourceProtos.Wifi.newBuilder()
                            .setBssid(entity.bssid)
                            .setSsid(entity.ssid)
                            .setFrequency(entity.frequency)
                            .setRssi(entity.rssi)
                            .build()
                    ).build()
            }
            else -> {
                null
            }
        }
    }

    fun retrieveWeather(latitude: Double, longitude: Double, year: Int, month: Int, day: Int, hour: Int): MeshupProtos.WeatherResponse {
        return getWeatherStub()
            .getWeather(
                MeshupProtos.WeatherRequest.newBuilder()
                    .setLatitude(latitude)
                    .setLongitude(longitude)
                    .setDateTime(CommonProtos.DateTime.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .setDay(day)
                        .setHour(hour)
                        .build()
                    )
                    .build()
            )
    }

    fun listExperiments(fromTimestamp: Long = 0,
                        toTimestamp: Long = System.currentTimeMillis(),
                        limit: Int = 30) : List<ExperimentProtos.ExperimentEssential> {
        return try {
           getExperimentStub()
                .listExperiments(
                    ApiProtos.ExperimentListQuery.newBuilder()
                        .setFromTimestamp(fromTimestamp)
                        .setToTimestamp(toTimestamp)
                        .setLimit(limit)
                        .build()
                ).asSequence().toList()
        } catch (e: StatusRuntimeException) {
            if(e.status.code == Status.Code.DEADLINE_EXCEEDED) throw TimeoutException()
            if(e.status.code == Status.Code.INTERNAL) throw ServerUnavailableException()
            if(e.status.code == Status.Code.CANCELLED) throw RequestCanceledException()
            throw GeneralException()
        }
    }

    fun getExperiment(experimentUuid: String) : ExperimentProtos.ExperimentFull {
        return try {
            getExperimentStub().getExperiment(
                ApiProtos.ExperimentOneQuery.newBuilder()
                    .setExperimentUuid(experimentUuid)
                    .build()
            )
        } catch (e: StatusRuntimeException) {
            if(e.status.code == Status.Code.DEADLINE_EXCEEDED) throw TimeoutException()
            if(e.status.code == Status.Code.INTERNAL) throw ServerUnavailableException()
            if(e.status.code == Status.Code.CANCELLED) throw RequestCanceledException()
            if(e.status.code == Status.Code.NOT_FOUND) throw NoCorrespondingExperimentException()
            throw GeneralException()
        }
    }

    @SuppressLint("CheckResult")
    fun participateExperiment(email: String, birthDate: YearMonthDay, isMale: Boolean, phoneNumber: String,
                              name: String, affiliation: String, experimentUuid: String, experimentGroup: String,
                              survey: String) {
        try {
            getExperimentStub().participateInExperiment(
                ExperimentProtos.Subject.newBuilder()
                    .setEmail(email)
                    .setBirthDate(
                        CommonProtos.Date.newBuilder()
                            .setYear(birthDate.year)
                            .setMonth(birthDate.month)
                            .setDay(birthDate.day)
                    )
                    .setGender(if (isMale) EnumProtos.GenderType.GENDER_MALE else EnumProtos.GenderType.GENDER_FEMALE)
                    .setPhoneNumber(phoneNumber)
                    .setName(name)
                    .setAffiliation(affiliation)
                    .setExperimentUuid(experimentUuid)
                    .setExperimentGroup(experimentGroup)
                    .setSurvey(survey)
                    .build()
            )

        } catch (e: StatusRuntimeException) {
            if(e.status.code == Status.Code.DEADLINE_EXCEEDED) throw TimeoutException()
            if(e.status.code == Status.Code.INTERNAL) throw ServerUnavailableException()
            if(e.status.code == Status.Code.CANCELLED) throw RequestCanceledException()
            if(e.status.code == Status.Code.INVALID_ARGUMENT) throw AlreadyClosedExperimentException()
            if(e.status.code == Status.Code.NOT_FOUND) throw NoCorrespondingExperimentException()
            throw GeneralException()
        }
    }

    @SuppressLint("CheckResult")
    fun dropOutExperiment(email: String, experimentUuid: String) {
        try {
            getExperimentStub().dropOutExperiment(
                ExperimentProtos.Subject.newBuilder()
                    .setEmail(email)
                    .setExperimentUuid(experimentUuid)
                    .build()
            )
        } catch (e: StatusRuntimeException) {
            if(e.status.code == Status.Code.DEADLINE_EXCEEDED) throw TimeoutException()
            if(e.status.code == Status.Code.INTERNAL) throw ServerUnavailableException()
            if(e.status.code == Status.Code.CANCELLED) throw RequestCanceledException()
            throw GeneralException()
        }
    }

    fun getParticipatedExperiment(email: String) : ExperimentProtos.ExperimentParticipated? {
        return try {
            val result = getExperimentStub().getParticipatedExperiment(
                ApiProtos.ExperimentOneQuery.newBuilder()
                    .setEmail(email)
                    .build()
            )
            if(TextUtils.isEmpty(result.subject.experimentUuid) || TextUtils.isEmpty(result.subject.email)) {
                null
            } else {
                result
            }
        } catch (e: StatusRuntimeException) {
            if(e.status.code == Status.Code.DEADLINE_EXCEEDED) throw TimeoutException()
            if(e.status.code == Status.Code.INTERNAL) throw ServerUnavailableException()
            if(e.status.code == Status.Code.CANCELLED) throw RequestCanceledException()
            throw GeneralException()
        }
    }

    fun uploadLog(logs: List<LogEntity>) {
        val countDownLatch = CountDownLatch(1)
        var exception: Throwable? = null

        val responseObserver = object : StreamObserver<CommonProtos.Empty> {
            override fun onNext(value: CommonProtos.Empty?) { }

            override fun onError(t: Throwable?) {
                exception = t
                countDownLatch.countDown()
            }

            override fun onCompleted() {
                countDownLatch.countDown()
            }
        }

        val requestObserver = getLogStub().uploadLog(responseObserver)

        try {
            for(log in logs) {
                requestObserver.onNext(
                    CommonProtos.Log.newBuilder()
                        .setEmail(log.email)
                        .setTime(
                            CommonProtos.Time.newBuilder()
                            .setTimestamp(log.timestamp)
                            .setUtcoffset(log.utcOffset)
                            .build()
                        )
                        .setTag(log.tag)
                        .setMessage(log.message)
                        .setSourceType(EnumProtos.DataSourceType.PHONE)
                        .setVersion(BuildConfig.VERSION_NAME)
                        .build()
                )
                SystemClock.sleep(50)

                if(countDownLatch.count == 0L) {
                    break
                }
            }
        } catch (e: Exception) {
            requestObserver.onError(e)
        }
        requestObserver.onCompleted()

        if(!countDownLatch.await(1L, TimeUnit.MINUTES)){
            throw ServerUnavailableException()
        }

        if(exception != null) {
            throw RuntimeException(exception)
        }
    }
}
*/