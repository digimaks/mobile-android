// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.qr_scan

import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lv.zzdats.commonfeature.features.issuance.IssuanceFlowUiConfig
import lv.zzdats.commonfeature.features.issuance.OfferConfigRepository
import lv.zzdats.commonfeature.features.offer.OfferUiConfig
import lv.zzdats.commonfeature.config.PresentationMode
import lv.zzdats.commonfeature.config.RequestUriConfig
import lv.zzdats.commonfeature.features.PresentationConfigStore
import lv.zzdats.corelogic.di.getOrCreatePresentationScope
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.uilogic.mvi.MviViewModel
import lv.zzdats.uilogic.mvi.ViewEvent
import lv.zzdats.uilogic.mvi.ViewSideEffect
import lv.zzdats.uilogic.mvi.ViewState
import lv.zzdats.uilogic.serializer.UiSerializer
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.InjectedParam
import lv.zzdats.resourceslogic.R
import lv.zzdats.uilogic.config.ConfigNavigation
import lv.zzdats.uilogic.config.NavigationType
import lv.zzdats.uilogic.navigation.DashboardScreens
import lv.zzdats.uilogic.navigation.DeepLinkType
import lv.zzdats.uilogic.navigation.IssuanceScreens
import lv.zzdats.uilogic.navigation.WebScreens
import lv.zzdats.uilogic.navigation.generateComposableArguments
import lv.zzdats.uilogic.navigation.generateComposableNavigationLink

data class State(
    val hasCameraPermission: Boolean = false,
    val shouldShowPermissionRational: Boolean = false,
    val finishedScanning: Boolean = false,
    val qrScannedConfig: QrScanUiConfig,

    val failedScanAttempts: Int = 0,
    val showInformativeText: Boolean = false,
    val informativeText: String,
) : ViewState

sealed class Event : ViewEvent {
    data object GoBack : Event()
    data class OnQrScanned(val resultQr: String) : Event()
    data object CameraAccessGranted : Event()
    data object ShowPermissionRational : Event()
    data object GoToAppSettings : Event()
}

sealed class Effect : ViewSideEffect {
    sealed class Navigation : Effect() {
        data class SwitchScreen(val screenRoute: String) : Navigation()
        data object Pop : Navigation()
        data object GoToAppSettings : Navigation()
    }
}

@KoinViewModel
class QrScanViewModel(
    private val interactor: QrScanInteractor,
    private val uiSerializer: UiSerializer,
    private val resourceProvider: ResourceProvider,
    @InjectedParam private val qrScannedConfig: String,
) : MviViewModel<Event, State, Effect>() {

    override fun setInitialState(): State {
        val deserializedConfig: QrScanUiConfig = uiSerializer.fromBase64(
            qrScannedConfig,
            QrScanUiConfig::class.java,
            QrScanUiConfig.Parser
        ) ?: throw RuntimeException("QrScanUiConfig:: is Missing or invalid")
        return State(
            qrScannedConfig = deserializedConfig,
            informativeText = calculateInformativeText(deserializedConfig.qrScanFlow)
        )
    }

    override fun handleEvents(event: Event) {
        when (event) {
            is Event.GoBack -> {
                setEffect {
                    Effect.Navigation.SwitchScreen(
                        screenRoute = generateComposableNavigationLink(
                            screen = WebScreens.Main,
                            arguments = generateComposableArguments(
                                mapOf("path" to "dashboard")
                            )
                        )
                    )
                }
            }

            is Event.OnQrScanned -> {
                if (viewState.value.finishedScanning) {
                    return
                }
                setState {
                    copy(finishedScanning = true)
                }

                handleScannedQr(event.resultQr)
            }

            is Event.CameraAccessGranted -> {
                setState {
                    copy(hasCameraPermission = true)
                }
            }

            is Event.ShowPermissionRational -> {
                setState {
                    copy(shouldShowPermissionRational = true)
                }
            }

            is Event.GoToAppSettings -> setEffect { Effect.Navigation.GoToAppSettings }
        }
    }

    private fun handleScannedQr(scannedQr: String) {
        viewModelScope.launch {
            val uri = Uri.parse(scannedQr)

            when (DeepLinkType.parse(uri.scheme.toString(), uri.host)) {
                DeepLinkType.OPENID4VP -> navigateToPresentationRequest(scannedQr)
                DeepLinkType.CREDENTIAL_OFFER -> navigateToIssuanceRequest(scannedQr, IssuanceFlowUiConfig.NO_DOCUMENT)
                else -> handleInvalidQr()
            }
        }
    }

    private fun handleInvalidQr() {
        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = WebScreens.QrInvalid.screenRoute
            )
        }
    }


    private fun calculateInformativeText(
        qrScanFlow: QrScanFlow,
    ): String {
        return with(resourceProvider) {
            when (qrScanFlow) {
                is QrScanFlow.Presentation -> getString(R.string.qr_scan_info_text_presentation_flow)
                is QrScanFlow.Issuance -> getString(R.string.qr_scan_info_text_issuance_flow)
            }
        }
    }

    private fun navigateToPresentationRequest(scanResult: String) {
        getOrCreatePresentationScope().close()
        getOrCreatePresentationScope()
        PresentationConfigStore.setConfig(
            RequestUriConfig(
                PresentationMode.OpenId4Vp(
                    uri = scanResult,
                    initiatorRoute = DashboardScreens.Dashboard.screenRoute
                )
            )
        )

        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = WebScreens.PresentationRequest,
                    arguments = generateComposableArguments(
                        mapOf("path" to "document-presentation")
                    )
                )
            )
        }
    }

    private fun navigateToIssuanceRequest(scanResult: String, issuanceFlow: IssuanceFlowUiConfig) {
        val offerConfig = OfferUiConfig(
            offerURI = scanResult,
            onSuccessNavigation = calculateOnSuccessNavigation(issuanceFlow),
            onCancelNavigation = calculateOnCancelNavigation(issuanceFlow)
        )
        OfferConfigRepository.setOfferConfig(offerConfig)

        setEffect {
            Effect.Navigation.SwitchScreen(
                screenRoute = generateComposableNavigationLink(
                    screen = WebScreens.DocumentOffer,
                    arguments = generateComposableArguments(
                        mapOf(
                            "uri" to scanResult
                        )
                    )
                )
            )
        }
    }

    private fun calculateOnSuccessNavigation(issuanceFlowUiConfig: IssuanceFlowUiConfig): ConfigNavigation {
        return when (issuanceFlowUiConfig) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PushRoute(
                        route = DashboardScreens.Dashboard.screenRoute,
                        popUpToRoute = IssuanceScreens.AddDocument.screenRoute
                    )
                )
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PopTo(
                        screen = DashboardScreens.Dashboard
                    )
                )
            }
        }
    }

    private fun calculateOnCancelNavigation(issuanceFlowUiConfig: IssuanceFlowUiConfig): ConfigNavigation {
        return when (issuanceFlowUiConfig) {
            IssuanceFlowUiConfig.NO_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.Pop
                )
            }

            IssuanceFlowUiConfig.EXTRA_DOCUMENT -> {
                ConfigNavigation(
                    navigationType = NavigationType.PopTo(
                        screen = DashboardScreens.Dashboard
                    )
                )
            }
        }
    }
}
