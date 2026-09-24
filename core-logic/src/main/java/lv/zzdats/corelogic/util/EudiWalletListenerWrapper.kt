// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.util

import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import java.net.URI

class EudiWalletListenerWrapper(
    private val onConnected: () -> Unit,
    private val onConnecting: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onError: (String) -> Unit,
    private val onQrEngagementReady: (String) -> Unit,
    private val onRequestReceived: (RequestProcessor.ProcessedRequest) -> Unit,
    private val onResponseSent: () -> Unit,
    private val onRedirect: (URI) -> Unit,
    private val intentToSend: () -> Unit
) : TransferEvent.Listener {
    override fun onTransferEvent(event: TransferEvent) {
        when (event) {
            is TransferEvent.Connected -> onConnected()
            is TransferEvent.Connecting -> onConnecting()
            is TransferEvent.Disconnected -> onDisconnected()
            is TransferEvent.Error -> onError(event.error.message ?: "")
            is TransferEvent.QrEngagementReady -> onQrEngagementReady(event.qrCode.content)
            is TransferEvent.RequestReceived -> onRequestReceived(event.processedRequest)
            is TransferEvent.ResponseSent -> onResponseSent()
            is TransferEvent.Redirect -> onRedirect(event.redirectUri)
            is TransferEvent.IntentToSend -> intentToSend()
        }
    }
}