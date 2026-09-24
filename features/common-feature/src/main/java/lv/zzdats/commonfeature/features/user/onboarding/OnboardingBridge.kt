// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.user.onboarding

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import kotlinx.coroutines.launch
import lv.zzdats.authlogic.controller.auth.BiometricsAvailability
import lv.zzdats.authlogic.controller.auth.DeviceAuthenticationResult
import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.controller.PrefKeys
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.BuildConfig
import lv.zzdats.commonfeature.features.auth.EparakstsAuthLauncher
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.corelogic.controller.IssuanceMethod
import lv.zzdats.corelogic.controller.IssueDocumentPartialState
import lv.zzdats.resourceslogic.bridge.ONBOARDING
import lv.zzdats.uilogic.navigation.NavigationCommand.ToNative
import lv.zzdats.uilogic.navigation.NavigationCommand.ToWeb
import lv.zzdats.uilogic.navigation.WebNavigationService
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.webbridge.UrlHandler
import lv.zzdats.webbridge.core.BaseBridge
import lv.zzdats.webbridge.core.BridgeRequest
import lv.zzdats.webbridge.core.BridgeResponse
import lv.zzdats.authlogic.service.AuthMethod

class OnboardingBridge(
    private val onboardingInteractor: OnboardingInteractor,
    private val navigationService: WebNavigationService,
    private val authService: AuthService,
    private val onboardingCoordinator: OnboardingCoordinator,
    private val deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    private val prefKeys: PrefKeys,
    private val logController: LogController,
) : BaseBridge(), UrlHandler {

    private companion object {
        const val TAG = "OnboardingBridge"
    }

    override fun getName() = ONBOARDING.BRIDGE_NAME

    override fun handleRequest(request: BridgeRequest): BridgeResponse {
        return when (request.function) {
            ONBOARDING.INITIATE_EPARAKSTS -> handleEParaksts(request)
            ONBOARDING.INITIATE_SMART_ID -> initiateSmartId(request)
            ONBOARDING.ACTIVATE_WALLET -> handleActivateWallet(request)
            ONBOARDING.INITIALISE_WALLET -> handleInitialiseWallet(request)
            else -> createErrorResponse(request, "Unknown function ${request.function}")
        }
    }

    private fun handleActivateWallet(request: BridgeRequest): BridgeResponse {
        prefKeys.setAppActivated(true)
        val response = createSuccessResponse(request, null)
        emitEvent(response)
        return response
    }

    fun handleAuthCode(code: String, state: String) {
        coroutineScope.launch {
            onboardingInteractor.handleAuthCode(code, state).collect { result ->
                when (result.status) {
                    is AuthPartialState.Success -> {
                        navigationService.navigate(
                            ToWeb(WebScreens.LOADING.path)
                        )
                    }

                    is AuthPartialState.Failure -> {
                        logController.e(TAG) { "Authorization callback failed: ${result.status.error}" }
                        navigationService.navigate(
                            ToWeb("document-offer-manual/error")
                        )
                    }
                }
            }
        }
    }

    private fun registerWalletInstance() {
        coroutineScope.launch {
            val host = findHostFragmentActivity() ?: return@launch
            onboardingInteractor.registerWallet(host)
        }
    }

    override fun handleUrl(request: WebResourceRequest): Boolean {
        val url = request.url.toString()

        if (EparakstsAuthLauncher.isEparakstsScheme(request.url.scheme)) {
            val context = webView?.context
            if (context == null || !EparakstsAuthLauncher.canHandleEparakstsApp(context)) {
                logController.i(TAG) { "Ignoring eParaksts app deep link because app is not available on device" }
                return true
            }

            val uri = request.url
            val successUrl = uri.getQueryParameter("successurl")
            val failureUrl = uri.getQueryParameter("failureurl")

            val originalParams = uri.queryParameterNames
            val newBuilder = Uri.Builder()
                .scheme(uri.scheme)
                .authority(uri.authority)
                .path(uri.path)

            for (paramName in originalParams) {
                if (paramName != "successurl" && paramName != "failureurl") {
                    for (value in uri.getQueryParameters(paramName)) {
                        newBuilder.appendQueryParameter(paramName, value)
                    }
                }
            }

            newBuilder.appendQueryParameter(
                "successurl",
                "${BuildConfig.DEEPLINK}resume_authn?url=${Uri.encode(successUrl ?: "")}"
            )
            newBuilder.appendQueryParameter(
                "failureurl",
                "${BuildConfig.DEEPLINK}resume_authn?url=${Uri.encode(failureUrl ?: "")}"
            )

            val modifiedUri = newBuilder.build()

            val intent = Intent(Intent.ACTION_VIEW, modifiedUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (error: Exception) {
                logController.e(TAG, error)
                coroutineScope.launch {
                    navigationService.navigate(ToWeb("document-offer-manual/error"))
                }
            }
            return true
        }
        return false
    }

    private fun handleEParaksts(request: BridgeRequest): BridgeResponse {
        prefKeys.setLastIssuanceMethod(IssuanceMethod.EPARAKSTS.value)
        coroutineScope.launch {
            val method = webView?.context?.let { context ->
                EparakstsAuthLauncher.preferredAuthMethod(context)
            } ?: AuthMethod.EPARAKSTS
            val url = authService.buildAuthUrl(method)
            Log.d("OnboardingBridge", "handleEParaksts: ${url.url}")

            webView?.context?.let { context ->
                EparakstsAuthLauncher.launch(context, url.url)
            }
        }
        return createSuccessResponse(request, null)
    }

    private fun initiateSmartId(request: BridgeRequest): BridgeResponse {
        prefKeys.setLastIssuanceMethod(IssuanceMethod.SMART_ID.value)
        coroutineScope.launch {
            val url = authService.buildAuthUrl(AuthMethod.SMARTID)
            webView?.context?.let { context ->
                EparakstsAuthLauncher.launch(context, url.url)
            }
        }
        return createSuccessResponse(request, null)
    }

    @SuppressLint("RestrictedApi")
    private fun handleInitialiseWallet(request: BridgeRequest): BridgeResponse {
        coroutineScope.launch {
            val fragmentActivity = findHostFragmentActivity()
            if (fragmentActivity == null) {
                emitEvent(createErrorResponse(request, "activity_missing"))
                return@launch
            }
            val registrationResult = onboardingInteractor.registerWallet(fragmentActivity)
            if (registrationResult.isFailure) {
                registrationResult.exceptionOrNull()?.let { logController.e(TAG, it) }
                val error = registrationResult.exceptionOrNull()?.localizedMessage
                    ?: "wallet_registration_failed"
                emitEvent(createErrorResponse(request, error))
                return@launch
            }

            val issuanceMethod = IssuanceMethod.fromValue(prefKeys.getLastIssuanceMethod())
                ?: IssuanceMethod.EPARAKSTS

            onboardingInteractor.issuePidDocument(prefKeys.getLastDocType(), issuanceMethod)
                .collect { result ->
                    prefKeys.setLastDocType("")
                    prefKeys.setLastIssuanceMethod("")

                    when (result) {
                        is IssueDocumentPartialState.Success -> {
                            emitEvent(createSuccessResponse(request, null)) {
                                coroutineScope.launch {
                                    navigationService.navigate(
                                        ToWeb(
                                            path = "document-offer-manual/success",
                                        )
                                    )
                                }
                            }
                        }

                        is IssueDocumentPartialState.Failure -> {
                            logController.e(TAG) { "Wallet issuance failed: ${result.errorMessage}" }
                            result.cause?.let { logController.e(TAG, it) }
                            emitEvent(createErrorResponse(request, null))
                        }

                        is IssueDocumentPartialState.UserAuthRequired -> {
                            val activity = findHostFragmentActivity()
                            activity?.let {
                                deviceAuthenticationInteractor.getBiometricsAvailability {
                                    when (it) {
                                        is BiometricsAvailability.CanAuthenticate -> {
                                            deviceAuthenticationInteractor.authenticateWithBiometrics(
                                                context = activity,
                                                crypto = result.crypto,
                                                notifyOnAuthenticationFailure = true,
                                                resultHandler = DeviceAuthenticationResult(
                                                    onAuthenticationSuccess = {
                                                        result.resultHandler.onAuthenticationSuccess()
                                                    },
                                                    onAuthenticationError = {
                                                        result.resultHandler.onAuthenticationError()
                                                        emitEvent(
                                                            createErrorResponse(
                                                                request,
                                                                "authentication_error"
                                                            )
                                                        )
                                                    },
                                                    onAuthenticationFailure = {
                                                        result.resultHandler.onAuthenticationFailure()
                                                        emitEvent(
                                                            createErrorResponse(
                                                                request,
                                                                "authentication_error"
                                                            )
                                                        )
                                                    }
                                                )
                                            )
                                        }

                                        is BiometricsAvailability.NonEnrolled -> {
                                            logController.e(TAG) { "Biometric authentication failed: biometrics not enrolled" }
                                            deviceAuthenticationInteractor.launchBiometricSystemScreen()
                                        }

                                        is BiometricsAvailability.Failure -> {
                                            logController.e(TAG) { "Biometric availability failed: ${it.errorMessage}" }
                                            result.resultHandler.onAuthenticationError()
                                            emitEvent(
                                                createErrorResponse(
                                                    request,
                                                    "authentication_error"
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (activity == null) {
                                logController.e(TAG) { "Biometric authentication failed: host activity missing" }
                                result.resultHandler.onAuthenticationError()
                                emitEvent(createErrorResponse(request, "activity_missing"))
                            }
                        }

                        is IssueDocumentPartialState.DeferredSuccess -> TODO()
                    }
                }
        }
        return createSuccessResponse(request, null)
    }
}
