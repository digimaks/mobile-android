// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.service

import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.wallet.EudiWallet
import org.koin.android.ext.android.inject
import eu.europa.ec.eudi.iso18013.transfer.engagement.NfcEngagementService as BaseService

class NfcEngagementService : BaseService() {

    val wallet: EudiWallet by inject()

    override val transferManager: TransferManager
        get() = wallet.transferManager
}