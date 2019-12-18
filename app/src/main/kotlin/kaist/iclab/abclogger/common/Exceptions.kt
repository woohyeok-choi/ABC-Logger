package kaist.iclab.abclogger.common

import kaist.iclab.abclogger.R

abstract class ABCException: Exception() {
    abstract fun getErrorStringRes() : Int
}

class GeneralException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_general_error
    }
}

class AlreadySignedInAccountException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_already_sign_in_account
    }
}

class SignInCanceledException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_cancel_sign_in_account
    }
}

class NoSignedAccountException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_signed_account
    }
}

class PermissionDeniedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_permission_denied
    }
}

class RuntimePermissionDeniedException(val requiredPermissions: Array<String>) : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_runtime_permission_denied
    }
}

class AppUsageDeniedException : ABCException () {
    override fun getErrorStringRes(): Int {
        return R.string.error_app_usage_denied
    }
}

class RuntimeAppUsageDeniedException : ABCException () {
    override fun getErrorStringRes(): Int {
        return R.string.error_runtime_app_usage_denied
    }
}

class NotificationAccessDeniedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_notification_denied
    }
}

class RuntimeNotificationAccessDeniedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_runtime_notification_denied
    }
}

class GoogleFitnessDeniedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_google_fitness_denied
    }
}

class RuntimeGoogleFitnessDeniedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_runtime_google_fitness_denied
    }
}

class FitnessDataSubscriptionFailedException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_failed_to_subscribe_fitness_data
    }
}

class NoSignedGoogleAccountException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_signed_fitness_account
    }
}

class NoParticipatedExperimentException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_participated_experiment
    }
}

class EmptySurveyException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_empty_survey
    }
}

class InvalidSurveyFormatException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_invalid_survey_format
    }
}

class NotVerifiedAccountException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_auth_not_verify
    }
}

class InvalidContentException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_invalid_content
    }
}

class InvalidUrlException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_invalid_url_format
    }
}

class EmptyEntityException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_empty_entity
    }
}

class NoNetworkAvailableException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_network_available
    }
}

class InvalidRequestException: ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_invalid_url_request
    }

}

class ServerUnavailableException: ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_server_unavailable
    }
}

class NoWifiNetworkAvailableException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_wifi_network_available
    }
}

class InternalAppException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_app_internal
    }
}

class TimeoutException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_too_long_time_request
    }
}

class InvalidJsonFormatException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_invalid_json_format
    }
}

class AuthFailureException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_auth_failure
    }
}

class RequestCanceledException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_request_canceled
    }
}

class NoCorrespondingExperimentException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_no_corresponding_experiment
    }
}

class AlreadyClosedExperimentException : ABCException() {
    override fun getErrorStringRes(): Int {
        return R.string.error_already_closed_experiment
    }
}
