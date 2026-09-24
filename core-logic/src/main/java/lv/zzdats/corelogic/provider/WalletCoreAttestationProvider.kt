// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.provider

import eu.europa.ec.eudi.openid4vci.Nonce
import eu.europa.ec.eudi.wallet.provider.WalletAttestationsProvider
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.crypto.SecureAreaRepository
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.networklogic.repository.WalletAttestationRepository
import org.multipaz.securearea.KeyInfo

interface WalletCoreAttestationProvider : WalletAttestationsProvider

class WalletCoreAttestationProviderImpl(
    private val walletCoreConfig: WalletConfig,
    private val walletAttestationRepository: WalletAttestationRepository,
    private val secureAreaRepository: SecureAreaRepository,
    private val prefKeys: PrefKeys
) : WalletCoreAttestationProvider {

    override suspend fun getWalletAttestation(
        keyInfo: KeyInfo
    ): Result<String> = walletAttestationRepository.getWalletAttestation(
        baseUrl = walletCoreConfig.walletProviderHost,
        keyInfo = keyInfo.publicKey.toJwk()
    )

    override suspend fun getKeyAttestation(
        keys: List<KeyInfo>,
        nonce: Nonce?
    ): Result<String> {
        if (!prefKeys.getWalletInstanceRegistered()) {
            return Result.failure(IllegalStateException("wallet_instance_not_registered"))
        }

        return walletAttestationRepository.getKeyAttestation(
            baseUrl = walletCoreConfig.walletProviderHost,
            keys = keys.map { it.publicKey.toJwk() },
            nonce = nonce?.value,
            hardwareKeyTag = secureAreaRepository.getHardwareKeyTag()
        )
    }
}
