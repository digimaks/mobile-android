// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.config

import lv.zzdats.networklogic.api.base.BaseApiClient
import lv.zzdats.networklogic.error.ApiErrorHandler
import lv.zzdats.networklogic.model.config.AppVersionConfigResponse

interface VersionApiClient {
    suspend fun getAppVersionConfig(): Result<AppVersionConfigResponse>
}

class VersionApiClientImpl(
    private val versionApi: VersionApi,
    private val errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), VersionApiClient {

    override suspend fun getAppVersionConfig(): Result<AppVersionConfigResponse> =
        handleRequest { versionApi.getAppVersionConfig() }
}