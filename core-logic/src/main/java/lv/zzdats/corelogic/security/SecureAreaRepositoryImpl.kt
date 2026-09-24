// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.security

import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.businesslogic.model.crypto.WalletInstanceData

/**
 * Implementation of [SecureAreaRepository] that delegates cryptographic key management
 * to [SecureAreaController].
 *
 * This class acts as a bridge between business logic and core security operations.
 */
class SecureAreaRepositoryImpl(
    private val secureAreaController: SecureAreaController
) : SecureAreaRepository {
    override suspend fun generateKeyWithAttestation(
        nonce: String,
        preferStrongBox: Boolean
    ): WalletInstanceData? {
        return secureAreaController.generateKeyWithAttestation(
            challenge = nonce,
            preferStrongBox = preferStrongBox
        )
    }

    override suspend fun checkInstance(): Boolean {
        return secureAreaController.checkInstance()
    }

    override suspend fun getHardwareKeyTag(): String? {
        return secureAreaController.getHardwareKeyTag()
    }

    override suspend fun deleteWalletInstance() {
        return secureAreaController.deleteKey()
    }
}
