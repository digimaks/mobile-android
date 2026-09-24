// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.config

import android.content.Context
import eu.europa.ec.eudi.wallet.EudiWalletConfig
import eu.europa.ec.eudi.wallet.issue.openid4vci.OpenId4VciManager
import eu.europa.ec.eudi.wallet.issue.openid4vci.dpop.DPopConfig
import eu.europa.ec.eudi.wallet.logging.Logger
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionAlgorithm
import eu.europa.ec.eudi.wallet.transfer.openId4vp.ClientIdScheme
import eu.europa.ec.eudi.wallet.transfer.openId4vp.EncryptionMethod
import eu.europa.ec.eudi.wallet.transfer.openId4vp.Format
import eu.europa.ec.eudi.wallet.transfer.openId4vp.PreregisteredVerifier
import lv.zzdats.businesslogic.config.EnvironmentConfig
import lv.zzdats.corelogic.BuildConfig
import lv.zzdats.resourceslogic.R
import kotlin.time.Duration.Companion.seconds

internal class WalletCoreConfigImpl(
    private val context: Context,
    private val environmentConfig: EnvironmentConfig,
) : WalletConfig {
    private var _config: EudiWalletConfig? = null

    override val config: EudiWalletConfig
        get() {
            if (_config == null) {
                _config = EudiWalletConfig {
                    configureOpenId4Vci {
                        withIssuerUrl(environmentConfig.getVciIssuerUrl())
                        withParUsage(OpenId4VciManager.Config.ParUsage.IF_SUPPORTED)
                        withDPopConfig(DPopConfig.Default)
                        withAuthFlowRedirectionURI(BuildConfig.ISSUE_AUTHORIZATION_DEEPLINK)
                        withClientAuthenticationType(OpenId4VciManager.ClientAuthenticationType.AttestationBased)
                    }
                    .configureLogging(
                        level = Logger.LEVEL_DEBUG
                    )
                    .configureOpenId4Vp {
                        withEncryptionAlgorithms(
                            EncryptionAlgorithm.ECDH_ES
                        )
                        withEncryptionMethods(
                            EncryptionMethod.A128CBC_HS256,
                            EncryptionMethod.A256GCM
                        )
                        withClientIdSchemes(
                            ClientIdScheme.X509SanDns,
                            ClientIdScheme.X509Hash,
                        )
                        withSchemes(
                            listOf(
                                BuildConfig.OPENID4VP_SCHEME,
                                BuildConfig.EUDI_OPENID4VP_SCHEME,
                                BuildConfig.MDOC_OPENID4VP_SCHEME,
                                BuildConfig.HAIP_OPENID4VP_SCHEME
                            )
                        )
                        withFormats(
                            Format.MsoMdoc.ES256, Format.SdJwtVc.ES256
                        )
                    }
                    .configureDocumentKeyCreation(
                        userAuthenticationRequired = true,
                        useStrongBoxForKeys = true,
                        userAuthenticationTimeout = 30.seconds
                    )
                    .configureReaderTrustStore(
                        context,
                        R.raw.eudi,
                        R.raw.cert,
                        R.raw.pidissuerca02_eu,
                        R.raw.pidissuerca02_ut,
                        R.raw.verifier,
                        R.raw.zzdats,
                        R.raw.prod_issuer_pem,
                        R.raw.dativa_verifier_dev,
                        R.raw.dativa_prod
                    )
                }
            }
            return _config!!
        }

    override val walletProviderHost: String
        get() = environmentConfig.getWalletApiHost()
}
