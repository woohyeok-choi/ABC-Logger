package kaist.iclab.abclogger.ui.survey

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kaist.iclab.abclogger.core.collector.DataRepository
import kaist.iclab.abclogger.core.ui.BaseViewModel
import kaist.iclab.abclogger.collector.survey.*
import kaist.iclab.abclogger.commons.AbcError
import kaist.iclab.abclogger.commons.EntityError
import kaist.iclab.abclogger.structure.survey.Survey
import kaist.iclab.abclogger.ui.State
import kaist.iclab.abclogger.ui.survey.list.SurveyPagingSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

class SurveyViewModel(
    private val dataRepository: DataRepository,
    savedStateHandle: SavedStateHandle,
    application: Application
) : BaseViewModel(savedStateHandle, application) {
    private val saveStatusChannel = Channel<State>()

    val saveStateFlow = saveStatusChannel.receiveAsFlow()

    fun get(id: Long) = flow {
        try {
            val survey =
                dataRepository.get<InternalSurveyEntity>(id) ?: throw EntityError.notFound()
            survey.responses = dataRepository.find {
                equal(InternalResponseEntity_.surveyId, id)
            }
            emit(State.Success(survey))
        } catch (e: Exception) {
            emit(State.Failure(AbcError.wrap(e)))
        }
    }

    suspend fun listAll() : Flow<PagingData<InternalSurveyEntity>> {
        prepareSync(System.currentTimeMillis())

        return Pager(PagingConfig(10)) {
            SurveyPagingSource(dataRepository) {
                orderDesc(InternalSurveyEntity_.actualTriggerTime)
            }
        }.flow
    }

    suspend fun listAnswered() : Flow<PagingData<InternalSurveyEntity>> {
        prepareSync(System.currentTimeMillis())

        return Pager(PagingConfig(10)) {
            SurveyPagingSource(dataRepository) {
                greater(InternalSurveyEntity_.responseTime, 0)
                    .or()
                    .equal(InternalSurveyEntity_.responseTime, 0)
                orderDesc(InternalSurveyEntity_.actualTriggerTime)
            }
        }.flow
    }

    suspend fun listNotAnswered() : Flow<PagingData<InternalSurveyEntity>> {
        prepareSync(System.currentTimeMillis())

        return Pager(PagingConfig(10)) {
            SurveyPagingSource(dataRepository) {
                less(InternalSurveyEntity_.responseTime, 0)
                orderDesc(InternalSurveyEntity_.actualTriggerTime)
            }
        }.flow
    }

    suspend fun listExpired() : Flow<PagingData<InternalSurveyEntity>> {
        prepareSync(System.currentTimeMillis())

        return Pager(PagingConfig(10)) {
            SurveyPagingSource(dataRepository) {
                val timestamp = System.currentTimeMillis()

                less(InternalSurveyEntity_.timeoutUntil, timestamp)
                equal(
                    InternalSurveyEntity_.timeoutAction,
                    Survey.TimeoutAction.DISABLED.ordinal.toLong()
                )
                orderDesc(InternalSurveyEntity_.actualTriggerTime)
            }
        }.flow
    }

    fun post(
        id: Long,
        responses: List<InternalResponseEntity>,
        reactionTime: Long,
        responseTime: Long
    ) = GlobalScope.launch(Dispatchers.IO) {
        try {
            saveStatusChannel.send(State.Loading)

            val survey =
                dataRepository.get<InternalSurveyEntity>(id) ?: throw EntityError.notFound()

            val updatedSurvey = survey.copy(
                isTransferredToSync = true,
                responseTime = responseTime,
                reactionTime = reactionTime,
            )

            dataRepository.put(updatedSurvey)
            dataRepository.put(responses)

            dataRepository.put(toSurveyEntity(updatedSurvey, responses))
            saveStatusChannel.send(State.Success(Unit))
        } catch (e: Exception) {
            saveStatusChannel.send(State.Failure(AbcError.wrap(e)))
        }
    }

    private suspend fun prepareSync(timestamp: Long) = withContext(ioContext) {
        try {
            val expiredEntities = dataRepository.find<InternalSurveyEntity> {
                equal(InternalSurveyEntity_.isTransferredToSync, false)
                less(InternalSurveyEntity_.timeoutUntil, timestamp)
                equal(
                    InternalSurveyEntity_.timeoutAction,
                    Survey.TimeoutAction.DISABLED.ordinal.toLong()
                )
            }.map { survey ->
                val responses = dataRepository.find<InternalResponseEntity> {
                    equal(InternalResponseEntity_.surveyId, survey.id)
                }
                toSurveyEntity(survey, responses)
            }
            dataRepository.put(expiredEntities)
        } catch (e: Exception) { }
    }

    private fun toSurveyEntity(
        survey: InternalSurveyEntity,
        responses: List<InternalResponseEntity>
    ) = SurveyEntity(
            eventTime = survey.eventTime,
            eventName = survey.eventName,
            intendedTriggerTime = survey.intendedTriggerTime,
            actualTriggerTime = survey.actualTriggerTime,
            reactionTime = survey.reactionTime,
            responseTime = survey.responseTime,
            url = survey.url,
            title = survey.title.main,
            altTitle = survey.title.alt,
            message = survey.message.main,
            altMessage = survey.message.alt,
            instruction = survey.instruction.main,
            altInstruction = survey.instruction.alt,
            timeoutUntil = survey.timeoutUntil,
            timeoutAction = survey.timeoutAction.name,
            responses = responses.map { response ->
                SurveyEntity.Response(
                    index = response.index,
                    type = response.question.option.type.name,
                    question = response.question.title.main,
                    altQuestion = response.question.title.alt,
                    answer = response.answer.main + response.answer.other
                )
            }
        ).apply {
            id = survey.id
        }
}
