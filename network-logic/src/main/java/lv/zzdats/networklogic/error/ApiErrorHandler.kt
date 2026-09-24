// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.error
import lv.zzdats.analyticslogic.provider.CrashlyticsProvider

import lv.zzdats.businesslogic.controller.log.LogController
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface ApiErrorHandler {
    fun handleError(response: Response<*>): Exception
    fun handleException(exception: Exception): Exception
}

class ApiErrorHandlerImpl(
    private val logController: LogController,
    private val crashlyticsProvider: CrashlyticsProvider? = null
) : ApiErrorHandler {
    override fun handleError(response: Response<*>): Exception {
        val errorBody = response.errorBody()?.string()
        val exception = when (response.code()) {
            400 -> ApiError.BadRequestException(errorBody)
            401 -> ApiError.UnauthorizedException(errorBody)
            403 -> ApiError.ForbiddenException(errorBody)
            422 -> {
                val tag = extractErrorTag(errorBody)
                ApiError.ValidationException(errorBody, tag)
            }
            500 -> {
                val err = ApiError.ServerException(errorBody)
                logController.e("ApiError") { "Server Error (500): $errorBody" }
                err
            }
            else -> ApiError.ApiException("Error ${response.code()}: $errorBody")
        }

        crashlyticsProvider?.logError(
            exception,
            "api_error status=${response.code()} path=${response.raw().request.url.encodedPath}"
        )
        return exception
    }

    override fun handleException(exception: Exception): Exception {
        crashlyticsProvider?.logError(exception, "api_exception")
        return when (exception) {
            is UnknownHostException -> ApiError.NetworkException("unknown_host")
            is SocketTimeoutException -> ApiError.NetworkException("timeout")
            is IOException -> ApiError.NetworkException("network_error")
            else -> ApiError.ApiException(null)
        }
    }

    private fun extractErrorTag(errorBody: String?): String? {
        if (errorBody == null) return null
        val tagPattern = "failed on the '(\\w+)' tag".toRegex()
        return tagPattern.find(errorBody)?.groupValues?.get(1)
    }
}

sealed class ApiError : Exception() {
    class BadRequestException(message: String?) : ApiError() {
        override val message: String? = message
    }
    class UnauthorizedException(message: String?) : ApiError() {
        override val message: String? = message
    }
    class ForbiddenException(message: String?) : ApiError() {
        override val message: String? = message
    }
    class ValidationException(
        message: String?,
        val errorTag: String? = null
    ) : ApiError() {
        override val message: String? = message
    }
    class ServerException(message: String?) : ApiError() {
        override val message: String? = message
    }
    class NetworkException(message: String?) : ApiError() {
        override val message: String? = message
    }
    class ApiException(message: String?) : ApiError() {
        override val message: String? = message
    }
}