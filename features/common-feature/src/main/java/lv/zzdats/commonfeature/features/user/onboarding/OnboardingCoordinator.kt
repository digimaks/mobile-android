// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.user.onboarding

import lv.zzdats.authlogic.controller.auth.OnboardingStorageController
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.networklogic.session.SessionManager
import lv.zzdats.networklogic.session.TokenStorage
import lv.zzdats.uilogic.navigation.Screen
import lv.zzdats.uilogic.navigation.WebScreens

class OnboardingCoordinator(
    private val walletCoreDocumentsController: WalletCoreDocumentsController,
    private val onboardingStorageController: OnboardingStorageController,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager,
    private val prefKeys: PrefKeys
) {
    suspend fun getCurrentScreen(): Screen {
        val hasDocuments = hasDocuments()
        val isActivated = prefKeys.getAppActivated()

        // Keep token/session state refreshed, but do not gate wallet activation flow on network/session.
        if (tokenStorage.hasToken()) {
            sessionManager.checkSession()
        }

        return when {
            !isActivated -> WebScreens.Activation
            !hasDocuments -> WebScreens.Main
            else -> WebScreens.Main
        }
    }

    private fun hasDocuments(): Boolean {
        return walletCoreDocumentsController.getAllDocuments().isNotEmpty()
    }
}
