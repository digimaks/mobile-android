// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.attestation

data class InstanceRq(
    val challenge: String,
    val key_attestation: String,
    val hardware_key_tag: String,
    val deviceIdentifiers: List<DeviceIdentifier>
)

data class DeviceIdentifier(
    val manufacturer: String,
    val model: String
)

data class NonceResp(
    val c_nonce: String
)

data class AttestationResp(
    val access_token: String,
    val wallet_attestations: List<WalletAttestation>
)
data class WalletAttestation(
    val format: String,
    val wallet_attestation: String
)