// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.webfeature.bridge

import android.webkit.WebResourceRequest
import kotlinx.coroutines.launch
import lv.zzdats.uilogic.navigation.NavigationCommand
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.webbridge.UrlHandler
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import lv.zzdats.webbridge.core.BridgeResponse.Status.SUCCESS

class ExternalLinkBridge(
    private val navigationService: WebNavigationService
) : BaseBridge(), UrlHandler {

    private val externalHosts = setOf("digimaks.eu", "www.digimaks.eu")

    override fun getName() = "externalLinkBridge"

    override fun handleRequest(request: BridgeRequest): BridgeResponse =
        BridgeResponse(id = request.id, status = SUCCESS)

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val host = request.url.host?.lowercase() ?: return false
        if (host in externalHosts) {
            coroutineScope.launch {
                navigationService.navigate(
                    NavigationCommand.ToExternal(request.url.toString())
                )
            }
            return true
        }
        return false
    }
}