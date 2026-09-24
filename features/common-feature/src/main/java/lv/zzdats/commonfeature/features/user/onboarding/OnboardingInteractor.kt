// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.user.onboarding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import lv.zzdats.authlogic.controller.auth.OnboardingStorageController
import lv.zzdats.authlogic.model.OnboardingState
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.IssueDocumentPartialState
import lv.zzdats.networklogic.api.attestation.AttestationApiClient
import lv.zzdats.networklogic.api.wallet.DocumentType
import lv.zzdats.networklogic.session.SessionManager
import lv.zzdats.networklogic.session.TokenStorage
import lv.zzdats.resourceslogic.bridge.DASHBOARD
import org.koin.core.annotation.Factory
import androidx.fragment.app.FragmentActivity
import lv.zzdats.corelogic.security.SecureAreaController
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationPolicy
import lv.zzdats.authlogic.model.BiometricCrypto
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.resourceslogic.provider.ResourceProvider
import kotlin.coroutines.resume
import androidx.biometric.BiometricPrompt
import eu.europa.ec.eudi.wallet.document.DocumentExtensions.DefaultKeyUnlockData
import lv.zzdats.networklogic.error.ErrorUtils
import lv.zzdats.networklogic.model.attestation.toInstanceRequest

sealed class AuthPartialState {
    data class Success(val url: String) : AuthPartialState()
    data class Failure(val error: String) : AuthPartialState()
}

data class AuthResult(
    val status: AuthPartialState,
)


interface OnboardingInteractor {
    fun handleAuthCode(code: String, state: String): Flow<AuthResult>
    fun getOnboardingState(): Flow<OnboardingState>
    fun issuePidDocument(docType: String, issuanceMethod: IssuanceMethod): Flow<IssueDocumentPartialState>
    suspend fun registerWallet(activity: FragmentActivity): Result<Unit>
}

@Factory
class OnboardingInteractorImpl(
    private val tokenStorage: TokenStorage,
    private val onboardingStorageController: OnboardingStorageController,
    private val sessionManager: SessionManager,
    private val authService: AuthService,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val attestationApiClient: AttestationApiClient,
    private val secureAreaController: SecureAreaController,
    private val resourceProvider: ResourceProvider,
    private val prefKeys: PrefKeys,
    private val logController: LogController
) : OnboardingInteractor {

    private companion object {
        const val TAG = "OnboardingInteractor"
    }

    override fun getOnboardingState() = flow {
        emit(onboardingStorageController.getOnboardingState())
    }

    override fun handleAuthCode(code: String, state: String): Flow<AuthResult> = flow {
        if (!authService.validateState(state)) {
            logController.e(TAG) { "Authorization callback rejected: invalid state" }
            emit(AuthResult(AuthPartialState.Failure("auth_failed_invalid_state")))
            return@flow
        }

        val codeVerifier = authService.getCodeVerifier(state)
        if (codeVerifier == null) {
            logController.e(TAG) { "Authorization callback rejected: code verifier missing for state=$state" }
            emit(AuthResult(AuthPartialState.Failure("auth_failed_missing_verifier")))
            return@flow
        }
        authService.getToken(code, codeVerifier).collect { result ->
            result.fold(
                onSuccess = { tokenResponse ->
                    tokenStorage.saveToken(tokenResponse.accessToken)
                    if (!sessionManager.checkSession()) {
                        emit(AuthResult(AuthPartialState.Failure("session_expired")))
                        return@fold
                    }
                    // Go straight to eparaksts/PID onboarding
                    emit(AuthResult(AuthPartialState.Success(DASHBOARD.SCREENS.MAIN)))
                },
                onFailure = { error ->
                    logController.e(TAG, error)
                    emit(AuthResult(AuthPartialState.Failure("Failed")))
                }
            )
        }
    }

    override fun issuePidDocument(
        docType: String,
        issuanceMethod: IssuanceMethod
    ): Flow<IssueDocumentPartialState> {
        val documentType = when (docType) {
            "pid" -> {
                DocumentType.PID
            }
            "mdl" -> {
                DocumentType.MDL
            }
            "rtu" -> {
                DocumentType.RTU
            }
            else -> {
                DocumentType.PID
            }
        }

        return deviceAuthenticationInteractor.issueDocumentByType(documentType, issuanceMethod)
    }

    override suspend fun registerWallet(activity: FragmentActivity): Result<Unit> = runCatching {
        val hasLocalInstance = attestationApiClient.checkInstance()
        if (hasLocalInstance && prefKeys.getWalletInstanceRegistered()) {
            return@runCatching
        }

        if (hasLocalInstance) {
            // Android Keystore attestation certs stay bound to the challenge used when the key was created.
            // Re-registration must mint a fresh attested key for the newly issued nonce.
            secureAreaController.deleteKey()
        }

        prefKeys.setWalletInstanceRegistered(false)
        val nonce = attestationApiClient.getNonce().getOrThrow()
        registerWalletInstance(activity, nonce.c_nonce, preferStrongBox = true).recoverCatching { error ->
            if (!error.isAttestationCertificateInvalid()) {
                throw error
            }

            logController.w(TAG) {
                "StrongBox attestation certificate validation failed, retrying wallet registration without StrongBox"
            }
            secureAreaController.deleteKey()
            registerWalletInstance(activity, nonce.c_nonce, preferStrongBox = false).getOrThrow()
        }.getOrThrow()
        prefKeys.setWalletInstanceRegistered(true)
    }.onFailure {
        prefKeys.setWalletInstanceRegistered(false)
        runCatching { secureAreaController.deleteKey() }
    }

    private suspend fun registerWalletInstance(
        activity: FragmentActivity,
        nonce: String,
        preferStrongBox: Boolean
    ): Result<Unit> = runCatching {
        val keyUnlockData =
            secureAreaController.prepareKeyUnlockData(
                challenge = nonce,
                preferStrongBox = preferStrongBox
            ) ?: error("key_unlock_prepare_failed")

        val cryptoObject = keyUnlockData.getCryptoObjectForSigning()
        authenticateForHardwareKey(
            activity = activity,
            cryptoObject = cryptoObject
        ).getOrThrow()

        val attestationData = secureAreaController.generateKeyWithAttestation(
            challenge = nonce,
            keyUnlockData = keyUnlockData,
            preferStrongBox = preferStrongBox
        )
        val instanceRequest = attestationData?.toInstanceRequest()
            ?: error("Invalid attestation request")

        attestationApiClient.submitInstance(instanceRequest).getOrThrow()
    }

    private fun Throwable.isAttestationCertificateInvalid(): Boolean =
        ErrorUtils.extractErrorCode(this) == ErrorUtils.ATTESTATION_CERTIFICATE_INVALID

    private suspend fun authenticateForHardwareKey(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject?
    ): Result<Unit> = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        deviceAuthenticationInteractor.authenticateWithBiometrics(
            context = activity,
            crypto = BiometricCrypto(cryptoObject),
            notifyOnAuthenticationFailure = true,
            policy = DeviceAuthenticationPolicy.HARDWARE_KEY,
            resultHandler = DeviceAuthenticationResult(
                onAuthenticationSuccess = {
                    if (cont.isActive) cont.resume(Result.success(Unit))
                },
                onAuthenticationError = {
                    if (cont.isActive) cont.resume(
                        Result.failure(IllegalStateException(resourceProvider.genericErrorMessage()))
                    )
                },
                onAuthenticationFailure = {
                    if (cont.isActive) cont.resume(
                        Result.failure(IllegalStateException(resourceProvider.genericErrorMessage()))
                    )
                }
            )
        )
    }
}
