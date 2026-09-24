// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.controller

import eu.europa.ec.eudi.wallet.logging.Logger
import lv.zzdats.businesslogic.controller.log.LogController

interface WalletCoreLogController : Logger

class WalletCoreLogControllerImpl(
    private val logController: LogController
) : WalletCoreLogController {

    override fun log(record: Logger.Record) {
        when (record.level) {
            Logger.LEVEL_ERROR -> record.thrown?.let { logController.e(it) }
                ?: logController.e { record.message }

            Logger.LEVEL_INFO -> logController.i { record.message }
            Logger.LEVEL_DEBUG -> logController.d { record.message }
        }
    }
}