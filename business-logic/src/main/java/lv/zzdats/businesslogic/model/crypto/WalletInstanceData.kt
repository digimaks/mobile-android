// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.businesslogic.model.crypto

/**
 * Hardware-backed attestation data used when registering a wallet instance.
 */
data class WalletInstanceData(
    val challenge: String,
    val keyAttestation: String,
    val hardwareKeyTag: String,
    val deviceIdentifiers: List<DeviceIdentifierData>
)

data class DeviceIdentifierData(
    val manufacturer: String,
    val model: String
)
