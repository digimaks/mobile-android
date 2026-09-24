// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.api.attestation

import lv.zzdats.networklogic.model.attestation.InstanceRq
import lv.zzdats.networklogic.model.attestation.NonceResp
import lv.zzdats.networklogic.model.attestation.AttestationResp
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface AttestationApi {
    @POST("wallet/nonce")
    suspend fun getNonce(): Response<NonceResp>

    @POST("wallet/instance")
    suspend fun getInstance(@Body rq: InstanceRq): Response<Unit>

    @POST("wallet/token")
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    suspend fun getWalletAttestation(
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:jwt-bearer",
        @Field("assertion") assertion: String
    ): Response<AttestationResp>
}