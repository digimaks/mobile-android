// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.security

import android.content.Context
import eu.europa.ec.eudi.wallet.EudiWallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.securearea.SecureArea
import org.multipaz.storage.Storage

class SecureAreaProvider private constructor() {

    suspend fun getSecureArea(wallet: EudiWallet): SecureArea {
        // Get the AndroidKeystoreSecureArea from the wallet's secure area repository
        return wallet.secureAreaRepository.getImplementation(AndroidKeystoreSecureArea.IDENTIFIER)
            ?: throw IllegalStateException("AndroidKeystoreSecureArea not found")
    }

    companion object {
        private var INSTANCE: SecureAreaProvider? = null

        fun getInstance(): SecureAreaProvider {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureAreaProvider().also { INSTANCE = it }
            }
        }
    }
}