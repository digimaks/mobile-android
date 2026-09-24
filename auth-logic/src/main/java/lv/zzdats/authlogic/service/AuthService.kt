// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.authlogic.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.authlogic.BuildConfig
import androidx.core.net.toUri
import lv.zzdats.businesslogic.controller.PrefsController
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.networklogic.model.wallet.TokenResponse
import org.json.JSONObject
import org.koin.core.annotation.Singleton

interface AuthService {
    fun buildAuthUrl(method: AuthMethod = AuthMethod.EPARAKSTS): AuthRequestData
    fun getToken(authCode: String, codeVerifier: String): Flow<Result<TokenResponse>>
    fun getCodeVerifier(state: String): String?
    fun validateState(receivedState: String): Boolean
}

data class AuthRequestData(
    val url: String,
    val codeVerifier: String,
    val state: String,
)

enum class AuthMethod {
    EPARAKSTS,
    EPARAKSTS_CROSS_DEVICE,
    SMARTID
}

@Singleton
class AuthServiceImpl(
    private val walletApiClient: WalletApiClient,
    private val prefsController: PrefsController,
    private val logController: LogController
) : AuthService {

    private companion object {
        const val TAG = "AuthService"
        const val PKCE_PREF_KEY = "PendingPkceCodeVerifiers"
        const val MAX_PENDING_PKCE_ENTRIES = 20
    }

    // Track verifiers per state to avoid races when multiple auth flows are launched.
    private val pendingCodeVerifiers = linkedMapOf<String, String>()
    private val pendingCodeVerifiersLock = Any()
    private var pendingCodeVerifiersHydrated = false

    override fun buildAuthUrl(method: AuthMethod): AuthRequestData {
        val codeVerifier = PkceUtils.generateCodeVerifier()
        val codeChallenge = PkceUtils.generateCodeChallenge(codeVerifier)
        val state = PkceUtils.generateRandomState()

        storeCodeVerifier(state = state, codeVerifier = codeVerifier)

        val acrValues = when (method) {
            AuthMethod.EPARAKSTS -> BuildConfig.EPARAKSTS_ACR_VALUES
            AuthMethod.EPARAKSTS_CROSS_DEVICE -> BuildConfig.EPARAKSTS_CROSS_DEVICE_ACR_VALUES
            AuthMethod.SMARTID -> BuildConfig.SMARTID_ACR_VALUES
        }

        val url = BuildConfig.EIDAS_AUTH_URL.toUri()
            .buildUpon()
            .appendPath("authorize")
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.EPARAKSTS_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.EPARAKSTS_REDIRECT_URI)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("acr_values", acrValues)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", "profile")
            .build()
            .toString()

        return AuthRequestData(url, codeVerifier, state)
    }

    override fun validateState(receivedState: String): Boolean {
        synchronized(pendingCodeVerifiersLock) {
            hydratePendingCodeVerifiersIfNeeded()
            return pendingCodeVerifiers.containsKey(receivedState)
        }
    }

    override fun getCodeVerifier(state: String): String? {
        // Remove after retrieval to prevent reuse.
        synchronized(pendingCodeVerifiersLock) {
            hydratePendingCodeVerifiersIfNeeded()
            val codeVerifier = pendingCodeVerifiers.remove(state)
            persistPendingCodeVerifiers()
            return codeVerifier
        }
    }

    override fun getToken(authCode: String, codeVerifier: String): Flow<Result<TokenResponse>> = flow {
        val redirectUri = BuildConfig.EPARAKSTS_REDIRECT_URI

        val result = walletApiClient.getToken(
            code = authCode,
            redirectUri = redirectUri,
            codeVerifier = codeVerifier
        )
        emit(result)
    }

    private fun storeCodeVerifier(state: String, codeVerifier: String) {
        synchronized(pendingCodeVerifiersLock) {
            hydratePendingCodeVerifiersIfNeeded()
            pendingCodeVerifiers[state] = codeVerifier
            while (pendingCodeVerifiers.size > MAX_PENDING_PKCE_ENTRIES) {
                pendingCodeVerifiers.entries.firstOrNull()?.key?.let { oldest ->
                    pendingCodeVerifiers.remove(oldest)
                } ?: break
            }
            persistPendingCodeVerifiers()
        }
    }

    private fun hydratePendingCodeVerifiersIfNeeded() {
        if (pendingCodeVerifiersHydrated) return

        pendingCodeVerifiers.clear()
        val serialized = prefsController.getString(PKCE_PREF_KEY, "")
        if (serialized.isNotBlank()) {
            runCatching {
                val jsonObject = JSONObject(serialized)
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    pendingCodeVerifiers[key] = jsonObject.optString(key)
                }
            }.onFailure { error ->
                logController.e(TAG, error)
                prefsController.clear(PKCE_PREF_KEY)
            }
        }
        pendingCodeVerifiersHydrated = true
    }

    private fun persistPendingCodeVerifiers() {
        runCatching {
            if (pendingCodeVerifiers.isEmpty()) {
                prefsController.clear(PKCE_PREF_KEY)
            } else {
                val jsonObject = JSONObject()
                pendingCodeVerifiers.forEach { (state, verifier) ->
                    jsonObject.put(state, verifier)
                }
                prefsController.setString(PKCE_PREF_KEY, jsonObject.toString())
            }
        }.onFailure { error ->
            logController.e(TAG, error)
        }
    }
}
