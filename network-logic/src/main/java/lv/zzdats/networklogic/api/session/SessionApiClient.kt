// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.session

import lv.zzdats.networklogic.api.base.BaseApiClient
import lv.zzdats.networklogic.error.ApiErrorHandler
import lv.zzdats.networklogic.model.session.SessionResponse
import lv.zzdats.networklogic.model.session.SessionStatus

interface SessionApiClient {
    suspend fun createSession(): Result<SessionResponse>
    suspend fun getStatus(): Result<SessionStatus>
}

class SessionApiClientImpl(
    private val sessionApi: SessionApi,
    private val errorHandler: ApiErrorHandler
) : BaseApiClient(errorHandler), SessionApiClient {

    override suspend fun createSession(): Result<SessionResponse> =
        handleRequest { sessionApi.createSession() }

    override suspend fun getStatus(): Result<SessionStatus> =
        handleRequest { sessionApi.getSessionStatus() }
}