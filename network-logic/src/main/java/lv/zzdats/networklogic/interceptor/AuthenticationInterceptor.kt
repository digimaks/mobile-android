// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.interceptor

import lv.zzdats.networklogic.session.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response

class AuthenticationInterceptor(
    private val tokenStorage: TokenStorage
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return tokenStorage.getToken()?.let { token ->
            val authenticatedRequest = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(authenticatedRequest)
        } ?: chain.proceed(request)
    }
}