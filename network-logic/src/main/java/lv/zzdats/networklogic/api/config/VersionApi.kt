// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.config

import lv.zzdats.networklogic.model.config.AppVersionConfigResponse
import retrofit2.Response
import retrofit2.http.GET

interface VersionApi {
    @GET("wallet/.well-known/appspecific/version.json")
    suspend fun getAppVersionConfig(): Response<AppVersionConfigResponse>
}