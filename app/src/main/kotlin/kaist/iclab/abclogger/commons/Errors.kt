package kaist.iclab.abclogger.commons

import android.content.Context
import com.github.kittinunf.fuel.core.FuelError
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.fitness.FitnessStatusCodes
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseApiNotAvailableException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.objectbox.exception.*
import kaist.iclab.abclogger.R
import polar.com.sdk.api.errors.*

open class AbcError(
    val typeRes: Int,
    val detailRes: Int? = null,
    message: String? = null
) : Exception(message) {
    open fun toString(context: Context): String {
        val type = context.getString(typeRes)
        val detail = detailRes?.let { context.getString(it) } ?: ""
        return "$type: $detail - $message"
    }

    fun toSimpleString(context: Context): String =
        detailRes?.let { context.getString(it) } ?: message ?: ""

    companion object {
        fun wrap(t: Throwable?) = when (t) {
            is ApiException -> GoogleApiError.fromApiException(t)
            is FirebaseException -> FirebaseError.fromFirebaseException(t)
            is FuelError -> HttpRequestError.fromFuelError(t)
            is DbException -> EntityError.fromDbException(t)
            is StatusRuntimeException -> SyncError.fromStatusRuntimeException(t)
            is AbcError -> t
            else -> PolarError.fromException(t) ?: AbcError(
                typeRes = R.string.general_unknown,
                message = t?.message
            )
        }
    }
}

open class GoogleApiError(
    detailRes: Int?,
    message: String? = null,
) : AbcError(R.string.error_google_api, detailRes, message) {
    class NoSignedAccount: GoogleApiError(R.string.error_google_api_no_signed_account)
    class StatusCode(detailRes: Int?, message: String?, val statusCode: Int?): GoogleApiError(detailRes, message)

    companion object {
        fun fromApiException(exception: ApiException): GoogleApiError {
            val message = exception.message
            val (detailRes, code) = when (exception.statusCode) {
                CommonStatusCodes.API_NOT_CONNECTED -> R.string.error_google_api_not_connected to "API_NOT_CONNECTED"
                CommonStatusCodes.CANCELED -> R.string.error_google_api_canceled to "CANCELED"
                CommonStatusCodes.DEVELOPER_ERROR -> R.string.error_google_api_developer_error to "DEVELOPER_ERROR"
                CommonStatusCodes.ERROR -> R.string.error_google_api_error to "ERROR"
                CommonStatusCodes.INTERNAL_ERROR -> R.string.error_google_api_internal_error to "INTERNAL_ERROR"
                CommonStatusCodes.INTERRUPTED -> R.string.error_google_api_interrupted to "INTERRUPTED"
                CommonStatusCodes.INVALID_ACCOUNT -> R.string.error_google_api_invalid_account to "INVALID_ACCOUNT"
                CommonStatusCodes.NETWORK_ERROR -> R.string.error_google_api_network_error to "NETWORK_ERROR"
                CommonStatusCodes.RESOLUTION_REQUIRED -> R.string.error_google_api_resolution_required to "RESOLUTION_REQUIRED"
                CommonStatusCodes.SIGN_IN_REQUIRED -> R.string.error_google_api_sign_in_required to "SIGN_IN_REQUIRED"
                CommonStatusCodes.TIMEOUT -> R.string.error_google_api_timeout to "TIMEOUT"
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> R.string.error_google_api_sign_in_cancelled to "SIGN_IN_CANCELLED"
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> R.string.error_google_api_sign_in_currently_in_progress to "SIGN_IN_CURRENTLY_IN_PROGRESS"
                GoogleSignInStatusCodes.SIGN_IN_FAILED -> R.string.error_google_api_sign_in_failed to "SIGN_IN_FAILED"
                FitnessStatusCodes.SUCCESS_NO_DATA_SOURCES -> R.string.error_google_api_success_no_data_sources to "SUCCESS_NO_DATA_SOURCES"
                FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED -> R.string.error_google_api_success_already_subscribed to "SUCCESS_ALREADY_SUBSCRIBED"
                FitnessStatusCodes.SUCCESS_NO_CLAIMED_DEVICE -> R.string.error_google_api_success_no_claimed_device to "SUCCESS_NO_CLAIMED_DEVICE"
                FitnessStatusCodes.SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES -> R.string.error_google_api_success_listener_not_registered_for_fitness_data_updates to "SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES"
                FitnessStatusCodes.NEEDS_OAUTH_PERMISSIONS -> R.string.error_google_api_needs_oauth_permissions to "NEEDS_OAUTH_PERMISSIONS"
                FitnessStatusCodes.CONFLICTING_DATA_TYPE -> R.string.error_google_api_conflicting_data_type to "CONFLICTING_DATA_TYPE"
                FitnessStatusCodes.INCONSISTENT_DATA_TYPE -> R.string.error_google_api_inconsistent_data_type to "INCONSISTENT_DATA_TYPE"
                FitnessStatusCodes.DATA_TYPE_NOT_FOUND -> R.string.error_google_api_data_type_not_found to "DATA_TYPE_NOT_FOUND"
                FitnessStatusCodes.APP_MISMATCH -> R.string.error_google_api_app_mismatch to "APP_MISMATCH"
                FitnessStatusCodes.UNKNOWN_AUTH_ERROR -> R.string.error_google_api_unknown_auth_error to "UNKNOWN_AUTH_ERROR"
                FitnessStatusCodes.UNSUPPORTED_PLATFORM -> R.string.error_google_api_unsupported_platform to "UNSUPPORTED_PLATFORM"
                FitnessStatusCodes.TRANSIENT_ERROR -> R.string.error_google_api_transient_error to "TRANSIENT_ERROR"
                FitnessStatusCodes.EQUIVALENT_SESSION_ENDED -> R.string.error_google_api_equivalent_session_ended to "EQUIVALENT_SESSION_ENDED"
                FitnessStatusCodes.APP_NOT_FIT_ENABLED -> R.string.error_google_api_app_not_fit_enabled to "APP_NOT_FIT_ENABLED"
                FitnessStatusCodes.API_EXCEPTION -> R.string.error_google_api_api_exception to "API_EXCEPTION"
                FitnessStatusCodes.AGGREGATION_NOT_SUPPORTED -> R.string.error_google_api_aggregation_not_supported to "AGGREGATION_NOT_SUPPORTED"
                FitnessStatusCodes.UNSUPPORTED_ACCOUNT -> R.string.error_google_api_unsupported_account to "UNSUPPORTED_ACCOUNT"
                FitnessStatusCodes.DISABLED_BLUETOOTH -> R.string.error_google_api_disabled_bluetooth to "DISABLED_BLUETOOTH"
                FitnessStatusCodes.INCONSISTENT_PACKAGE_NAME -> R.string.error_google_api_inconsistent_package_name to "INCONSISTENT_PACKAGE_NAME"
                FitnessStatusCodes.DATA_SOURCE_NOT_FOUND -> R.string.error_google_api_data_source_not_found to "DATA_SOURCE_NOT_FOUND"
                FitnessStatusCodes.DATASET_TIMESTAMP_INCONSISTENT_WITH_UPDATE_TIME_RANGE -> R.string.error_google_api_dataset_timestamp_inconsistent_with_update_time_range to "DATASET_TIMESTAMP_INCONSISTENT_WITH_UPDATE_TIME_RANGE"
                FitnessStatusCodes.INVALID_SESSION_TIMESTAMPS -> R.string.error_google_api_invalid_session_timestamps to "INVALID_SESSION_TIMESTAMPS"
                FitnessStatusCodes.INVALID_DATA_POINT -> R.string.error_google_api_invalid_data_point to "INVALID_DATA_POINT"
                FitnessStatusCodes.INVALID_TIMESTAMP -> R.string.error_google_api_invalid_timestamp to "INVALID_TIMESTAMP"
                FitnessStatusCodes.DATA_TYPE_NOT_ALLOWED_FOR_API -> R.string.error_google_api_data_type_not_allowed_for_api to "DATA_TYPE_NOT_ALLOWED_FOR_API"
                FitnessStatusCodes.REQUIRES_APP_WHITELISTING -> R.string.error_google_api_requires_app_whitelisting to "REQUIRES_APP_WHITELISTING"
                else -> null to null
            }
            return StatusCode(detailRes, message, exception.statusCode)
        }

        fun noSignedAccount() = GoogleApiError(R.string.error_google_api_no_signed_account)
    }
}

class FirebaseError(
    detailRes: Int?,
    message: String? = null,
    code: String? = null,
    reason: String? = null,
    email: String? = null
) : AbcError(R.string.error_firebase, detailRes, message) {
    companion object {
        fun fromFirebaseException(exception: FirebaseException): FirebaseError {
            val message = exception.message
            var detailRes: Int? = null
            var code: String? = null
            var reason: String? = null
            var email: String? = null

            when (exception) {
                is FirebaseAuthActionCodeException -> {
                    detailRes = R.string.error_firebase_auth_action_code
                    code = exception.errorCode
                }
                is FirebaseAuthEmailException -> {
                    detailRes = R.string.error_firebase_auth_email
                    code = exception.errorCode
                }
                is FirebaseAuthInvalidCredentialsException -> {
                    detailRes = R.string.error_firebase_auth_invalid_credentials
                    code = exception.errorCode
                }
                is FirebaseAuthInvalidUserException -> {
                    detailRes = R.string.error_firebase_auth_invalid_user
                    code = exception.errorCode
                }
                is FirebaseAuthRecentLoginRequiredException -> {
                    detailRes = R.string.error_firebase_auth_recent_login_required
                    code = exception.errorCode
                }
                is FirebaseAuthUserCollisionException -> {
                    detailRes = R.string.error_firebase_auth_user_collision
                    code = exception.errorCode
                    email = exception.email
                }
                is FirebaseAuthWeakPasswordException -> {
                    detailRes = R.string.error_firebase_auth_weak_password
                    code = exception.errorCode
                    reason = exception.reason
                }
                is FirebaseAuthMultiFactorException -> {
                    detailRes = R.string.error_firebase_auth_multi_factor
                    code = exception.errorCode
                }
                is FirebaseAuthWebException -> {
                    detailRes = R.string.error_firebase_auth_web
                    code = exception.errorCode
                }
                is FirebaseApiNotAvailableException -> {
                    detailRes = R.string.error_firebase_api_not_available
                }
                is FirebaseNetworkException -> {
                    detailRes = R.string.error_firebase_network
                }
                is FirebaseTooManyRequestsException -> {
                    detailRes = R.string.error_firebase_too_many_requests
                }
            }

            return FirebaseError(detailRes, message, code, reason, email)
        }
    }
}

open class PolarError(
    detailRes: Int?,
    message: String? = null
) : AbcError(R.string.error_polar, detailRes, message) {
    class NoSetting : PolarError(R.string.error_polar_no_setting_available)
    class Device(detailRes: Int?, message: String?): PolarError(detailRes, message)

    companion object {
        fun fromException(exception: Throwable?): PolarError? {
            val message = exception?.message
            val detailRes = when (exception) {
                is PolarDeviceDisconnected -> R.string.error_polar_device_disconnected
                is PolarDeviceNotConnected -> R.string.error_polar_device_not_connected
                is PolarDeviceNotFound -> R.string.error_polar_device_not_found
                is PolarInvalidArgument -> R.string.error_polar_invalid_argument
                is PolarOperationNotSupported -> R.string.error_polar_operation_not_supported
                is PolarServiceNotAvailable -> R.string.error_polar_service_not_available
                else -> null
            } ?: return null

            return Device(detailRes, message)
        }

        fun noSetting() = NoSetting()
    }
}

open class HttpRequestError(
    detailRes: Int?,
    message: String? = null
) : AbcError(R.string.error_http, detailRes, message) {
    class InvalidUrl: HttpRequestError(R.string.error_precondition_invalid_url)
    class EmptyContent: HttpRequestError(R.string.error_precondition_empty_content)
    class InvalidJsonFormat: HttpRequestError(R.string.error_precondition_invalid_format)
    class StatusCode(detailRes: Int?, message: String? = null, val statusCode: Int? = null): HttpRequestError(detailRes, message)

    companion object {
        fun invalidUrl() = InvalidUrl()
        fun emptyContent() = EmptyContent()
        fun invalidJsonFormat() = InvalidJsonFormat()

        fun fromFuelError(exception: FuelError): HttpRequestError {
            val detailRes = when (exception.response.statusCode) {
                400 -> R.string.error_http_400
                401 -> R.string.error_http_401
                403 -> R.string.error_http_403
                404 -> R.string.error_http_404
                405 -> R.string.error_http_405
                406 -> R.string.error_http_406
                408 -> R.string.error_http_408
                410 -> R.string.error_http_410
                411 -> R.string.error_http_411
                412 -> R.string.error_http_412
                413 -> R.string.error_http_413
                414 -> R.string.error_http_414
                415 -> R.string.error_http_415
                429 -> R.string.error_http_429
                500 -> R.string.error_http_500
                501 -> R.string.error_http_501
                502 -> R.string.error_http_502
                503 -> R.string.error_http_503
                504 -> R.string.error_http_504
                511 -> R.string.error_http_511
                else -> null
            }
            return StatusCode(detailRes, exception.message, exception.response.statusCode)
        }
    }
}

open class PreconditionError(
    detailRes: Int?
) : AbcError(R.string.error_precondition, detailRes) {
    class PermissionDenied : PreconditionError(R.string.error_precondition_permission_denied)
    class BackgroundLocationAccessDenied :
        PreconditionError(R.string.error_precondition_background_location_access_denied)

    class BatteryOptimizationIgnoreDenied :
        PreconditionError(R.string.error_precondition_battery_optimization_ignore_denied)

    class OutdatedGooglePlayService :
        PreconditionError(R.string.error_precondition_outdated_google_play_service)

    companion object {
        fun permissionDenied() = PermissionDenied()

        fun backgroundLocationAccessDenied() = BackgroundLocationAccessDenied()

        fun whiteListDenied() = BatteryOptimizationIgnoreDenied()

        fun googlePlayServiceOutdated() = OutdatedGooglePlayService()
    }
}

open class CollectorError(
    detailRes: Int?,
    open val qualifiedName: String? = null
) : AbcError(R.string.error_collector, detailRes) {
    class NotFound(override val qualifiedName: String) : CollectorError(
        R.string.error_collector_not_found,
        qualifiedName = qualifiedName
    )

    class SettingChangedDuringOperating(override val qualifiedName: String) : CollectorError(
        R.string.error_collector_change_setting_during_operating,
        qualifiedName = qualifiedName
    )

    class TurningOnRequestWhenUnavailable(override val qualifiedName: String) : CollectorError(
        R.string.error_collector_turning_on_request_when_unavailable,
        qualifiedName = qualifiedName
    )

    companion object {
        fun notFound(qualifiedName: String) = NotFound(qualifiedName)
        fun turningOnRequestWhenUnavaiable(qualifiedName: String) = TurningOnRequestWhenUnavailable(qualifiedName)
        fun changeSettingDuringOperating(qualifiedName: String) =
            SettingChangedDuringOperating(qualifiedName)
    }
}

/**
 * This error represents any data read-write related-error
 */
open class EntityError(
    detailRes: Int?,
    message: String? = null
) : AbcError(R.string.error_entity, detailRes, message) {
    class NotFound : EntityError(R.string.error_entity_not_found, null)
    class EmptyData : EntityError(R.string.error_entity_empty_data, null)
    class Database(detailRes: Int?, message: String?): EntityError(detailRes, message)
    companion object {
        fun notFound() = NotFound()
        fun emptyData() = EmptyData()
        fun fromDbException(exception: DbException): EntityError {
            val res = when (exception) {
                is DbFullException -> R.string.error_entity_db_full
                is FileCorruptException -> R.string.error_entity_file_corrupted
                is DbShutdownException -> R.string.error_entity_db_shutdown
                is DbDetachedException -> R.string.error_entity_db_detached
                else -> R.string.error_entity_unknown_error
            }
            return Database(res, exception.message)
        }
    }
}

open class SyncError(
    detailRes: Int?,
    message: String? = null,
    open val isRetry: Boolean
) : AbcError(R.string.error_sync, detailRes, message) {
    class UnavailableNetwork : SyncError(R.string.error_sync_network_unavailable, null, true)
    class StatusCode(
        detailRes: Int?,
        message: String? = null,
        override val isRetry: Boolean,
        val code: Status.Code?
    ) : SyncError(detailRes, message, isRetry)

    companion object {
        fun unavailableNetwork() = UnavailableNetwork()

        fun fromStatusRuntimeException(exception: StatusRuntimeException): SyncError {
            val (res, isRetry) = when (exception.status.code) {
                Status.Code.CANCELLED -> R.string.error_sync_canceled to false
                Status.Code.INVALID_ARGUMENT -> R.string.error_sync_invalid_argument to false
                Status.Code.DEADLINE_EXCEEDED -> R.string.error_sync_deadline_exceeded to true
                Status.Code.NOT_FOUND -> R.string.error_sync_not_found to false
                Status.Code.ALREADY_EXISTS -> R.string.error_sync_already_exists to false
                Status.Code.PERMISSION_DENIED -> R.string.error_sync_permission_denied to false
                Status.Code.RESOURCE_EXHAUSTED -> R.string.error_sync_resource_exhausted to true
                Status.Code.FAILED_PRECONDITION -> R.string.error_sync_failed_precondition to false
                Status.Code.ABORTED -> R.string.error_sync_aborted to true
                Status.Code.OUT_OF_RANGE -> R.string.error_sync_out_of_range to false
                Status.Code.UNIMPLEMENTED -> R.string.error_sync_unimplemented to false
                Status.Code.INTERNAL -> R.string.error_sync_internal to true
                Status.Code.UNAVAILABLE -> R.string.error_sync_unavailable to true
                Status.Code.DATA_LOSS -> R.string.error_sync_data_loss to true
                Status.Code.UNAUTHENTICATED -> R.string.error_sync_unauthenticated to false
                else -> R.string.error_sync_unknown to true
            }
            return StatusCode(res, exception.status.description, isRetry, exception.status.code)
        }
    }
}