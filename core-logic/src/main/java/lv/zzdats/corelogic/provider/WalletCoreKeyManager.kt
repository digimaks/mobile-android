// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.corelogic.provider

import android.content.Context
import android.util.Base64
import eu.europa.ec.eudi.wallet.provider.SecureAreaWalletKeyManager
import eu.europa.ec.eudi.wallet.provider.WalletAttestationKey
import eu.europa.ec.eudi.wallet.provider.WalletKeyManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.bytestring.ByteString
import lv.zzdats.networklogic.api.attestation.AttestationApiClient
import org.multipaz.crypto.Algorithm
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.storage.android.AndroidStorage
import java.security.MessageDigest
import kotlin.time.ExperimentalTime

class WalletCoreKeyManager(
    private val context: Context,
    private val attestationApiClient: AttestationApiClient,
) : WalletKeyManager {

    private var delegate: SecureAreaWalletKeyManager? = null
    private val mutex = Mutex()

    override suspend fun getOrCreateWalletAttestationKey(
        issuerUrl: String,
        supportedAlgorithms: List<Algorithm>,
    ): Result<WalletAttestationKey> {
        return getDelegate().getOrCreateWalletAttestationKey(
            issuerUrl = issuerUrl,
            supportedAlgorithms = supportedAlgorithms,
        )
    }

    override suspend fun getWalletAttestationKey(keyAlias: String): WalletAttestationKey? {
        return getDelegate().getWalletAttestationKey(keyAlias)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun getDelegate(): SecureAreaWalletKeyManager {
        return delegate ?: mutex.withLock {
            delegate ?: run {
                val storage = AndroidStorage("${context.noBackupFilesDir.path}/wallet-attest.bin")
                val secureArea = AndroidKeystoreSecureArea.create(storage)

                SecureAreaWalletKeyManager(
                    secureArea = secureArea,
                    createKeySettingsProvider = { algorithm ->
                        val challenge = fetchAttestationChallengeBytes()
                        AndroidKeystoreCreateKeySettings.Builder(ByteString(challenge))
                            .setAlgorithm(algorithm)
                            .build()
                    }
                ).also { delegate = it }
            }
        }
    }

    private suspend fun fetchAttestationChallengeBytes(): ByteArray {
        val nonce = attestationApiClient.getNonce().getOrThrow().c_nonce
        return nonce.toAttestationChallengeBytes()
    }

    private fun String.toAttestationChallengeBytes(): ByteArray {
        val decoded = runCatching {
            Base64.decode(this, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }.getOrNull()

        val raw = decoded?.takeIf { it.isNotEmpty() } ?: this.toByteArray(Charsets.UTF_8)

        // Keep challenge size within keystore-friendly bounds deterministically.
        return if (raw.size <= 128) raw else MessageDigest.getInstance("SHA-256").digest(raw)
    }
}
