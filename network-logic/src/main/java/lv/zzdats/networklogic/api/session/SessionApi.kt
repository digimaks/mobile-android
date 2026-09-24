// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.session

import lv.zzdats.networklogic.model.session.SessionResponse
import lv.zzdats.networklogic.model.session.SessionStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SessionApi {
    @POST("session")
    suspend fun createSession(): Response<SessionResponse>

    @GET("session")
    suspend fun getSessionStatus(): Response<SessionStatus>
}