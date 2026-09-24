// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.base

import lv.zzdats.networklogic.error.ApiErrorHandler
import retrofit2.Response

open class BaseApiClient(private val errorHandler: ApiErrorHandler) {

    protected suspend fun <T> handleRequest(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                if (response.code() == 204) {
                    @Suppress("UNCHECKED_CAST")
                    Result.success(Unit as T)
                } else {
                    Result.success(response.body()!!)
                }
            } else {
                Result.failure(errorHandler.handleError(response))
            }
        } catch (e: Exception) {
            Result.failure(errorHandler.handleException(e))
        }
    }
}