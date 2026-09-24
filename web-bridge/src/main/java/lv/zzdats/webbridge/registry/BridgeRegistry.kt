// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webbridge.registry

import lv.zzdats.webbridge.core.WebBridgeInterface

interface BridgeProvider {
    fun provideBridges(): List<WebBridgeInterface>
}

class BridgeRegistry(
    private val bridgeProviders: List<BridgeProvider>
) {
    fun getAllBridges(): List<WebBridgeInterface> =
        bridgeProviders.flatMap { it.provideBridges() }
}