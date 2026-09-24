// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.session

import lv.zzdats.businesslogic.controller.PrefsController

interface TokenStorage {
    fun saveToken(token: String)
    fun getToken(): String?
    fun hasToken(): Boolean
    fun clearToken()
}

class SecureTokenStorage(
    private val prefsController: PrefsController
) : TokenStorage {

    override fun saveToken(token: String) {
        prefsController.setString(PREF_SESSION_TOKEN, token)
    }

    override fun getToken(): String? {
        return prefsController.getString(PREF_SESSION_TOKEN, "")
    }

    override fun hasToken(): Boolean {
        return prefsController.contains(PREF_SESSION_TOKEN)
    }

    override fun clearToken() {
        prefsController.clear(PREF_SESSION_TOKEN)
    }

    companion object {
        private const val PREF_SESSION_TOKEN = "session_token"
    }
}