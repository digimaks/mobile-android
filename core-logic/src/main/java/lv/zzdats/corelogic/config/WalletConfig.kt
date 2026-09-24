// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.config

import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.document.CreateDocumentSettings.CredentialPolicy
import lv.zzdats.corelogic.model.DocumentIdentifier
import java.time.Duration

interface WalletConfig {
    val config: EudiWalletConfig

    /**
     * The interval at which revocations are checked.
     *
     * This property defines the time interval between checks for revoked tokens or credentials.
     * It is currently set to 15 minutes.
     * Androids WorkManager enforces a minimum repeat interval of 15 minutes on API 23+,
     * so this value cannot be lower than 15.
     */
    val revocationInterval: Duration get() = Duration.ofMinutes(15)

    val documentIssuanceConfig: DocumentIssuanceConfig
        get() = DocumentIssuanceConfig(
            defaultRule = DocumentIssuanceRule(
                policy = CredentialPolicy.RotateUse,
                numberOfCredentials = 1
            ),
            documentSpecificRules = mapOf(
                DocumentIdentifier.MdocPid to DocumentIssuanceRule(
                    policy = CredentialPolicy.RotateUse,
                    numberOfCredentials = 10
                ),
                DocumentIdentifier.SdJwtPid to DocumentIssuanceRule(
                    policy = CredentialPolicy.RotateUse,
                    numberOfCredentials = 10
                ),
            ),
            reissuanceRule = ReIssuanceRule(
                minNumberOfCredentials = 2,
                minExpirationHours = 24,
                backgroundInterval = Duration.ofMinutes(15)
            )
        )

    val walletProviderHost: String
}
