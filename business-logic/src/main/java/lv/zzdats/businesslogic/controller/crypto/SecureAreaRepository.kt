// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.controller.crypto

import lv.zzdats.businesslogic.model.crypto.WalletInstanceData

/**
 * Interface for handling secure key management and attestation using Secure Area.
 */
interface SecureAreaRepository {
    suspend fun generateKeyWithAttestation(
        nonce: String,
        preferStrongBox: Boolean = true
    ): WalletInstanceData?

    suspend fun getHardwareKeyTag(): String?

    suspend fun checkInstance(): Boolean

    suspend fun deleteWalletInstance()
}
