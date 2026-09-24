// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.interceptor

import lv.zzdats.businesslogic.provider.AppInstanceIdProvider
import okhttp3.Interceptor
import okhttp3.Response

class AppInstanceIdInterceptor(
    private val appInstanceIdProvider: AppInstanceIdProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(APP_INSTANCE_ID_HEADER, appInstanceIdProvider.getAppInstanceId())
            .build()

        return chain.proceed(request)
    }

    companion object {
        const val APP_INSTANCE_ID_HEADER = "X-App-Instance-Id"
    }
}
