// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.networklogic.model.attestation

import lv.zzdats.businesslogic.model.crypto.WalletInstanceData

fun WalletInstanceData.toInstanceRequest(): InstanceRq =
    InstanceRq(
        challenge = challenge,
        key_attestation = keyAttestation,
        hardware_key_tag = hardwareKeyTag,
        deviceIdentifiers = deviceIdentifiers.map {
            DeviceIdentifier(
                manufacturer = it.manufacturer,
                model = it.model
            )
        }
    )
