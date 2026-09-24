// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lv.zzdats.networklogic.api.session.SessionApi
import lv.zzdats.networklogic.api.session.SessionApiClient
import org.koin.core.annotation.Singleton


@Singleton
class SessionManager(
    private val sessionApiClient: SessionApiClient,
    private val tokenStorage: TokenStorage
) : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext = job + Dispatchers.IO

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.NotInitialized)
    suspend fun checkSession(): Boolean {
        if (!tokenStorage.hasToken()) return false

        return sessionApiClient.getStatus()
            .onSuccess { status ->
                if (status.active) {
                    _sessionState.value = SessionState.Active(status.secondsToLive)
                    return true
                } else {
                    _sessionState.value = SessionState.Expired
                    tokenStorage.clearToken()
                }
            }
            .onFailure {
                _sessionState.value = SessionState.Error("Session expired")
                tokenStorage.clearToken()
            }
            .isSuccess
    }
}

sealed class SessionState {
    object NotInitialized : SessionState()
    data class Active(val secondsToLive: Int) : SessionState()
    object Expired : SessionState()
    data class Error(val message: String) : SessionState()
}