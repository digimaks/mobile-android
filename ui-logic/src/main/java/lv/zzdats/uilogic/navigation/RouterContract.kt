// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.navigation

import lv.zzdats.resourceslogic.bridge.ONBOARDING

interface NavigatableItem

open class Screen(name: String, parameters: String = "", webPath: String = "") : NavigatableItem {
    val screenRoute: String = name + parameters
    val screenName = name
    val path = webPath
}

sealed class StartupScreens {
    data object Splash : Screen(name = "SPLASH")
    data object ForceUpdate : Screen(
        name = "FORCE_UPDATE",
        parameters = "?storeUrl={storeUrl}"
    )
}

sealed class CommonScreens {
    data object Success : Screen(
        name = "SUCCESS",
        parameters = "?successConfig={successConfig}"
    )

    data object Biometric : Screen(
        name = "BIOMETRIC",
        parameters = "?biometricConfig={biometricConfig}"
    )

    data object QrScan : Screen(
        name = "QR_SCAN",
        parameters = "?qrScanConfig={qrScanConfig}"
    )

    data object SecurityError : Screen(
        name = "SECURITY_ERROR",
        parameters = "?reason={reason}"
    )
}

sealed class IssuanceScreens {
    data object AddDocument : Screen(
        name = "ISSUANCE_ADD_DOCUMENT",
        parameters = "?flowType={flowType}"
    )

    data object AddPid : Screen(
        name = "ISSUANCE_ADD_PID",
        parameters = "?flowType={flowType}"
    )

    data object Success : Screen(
        name = "ISSUANCE_SUCCESS",
        parameters = "?flowType={flowType}"
                + "&documentId={documentId}"
    )

    data object PidSuccess : Screen(
        name = "ISSUANCE_PID_SUCCESS",
        parameters = "?flowType={flowType}"
                + "&documentId={documentId}"
    )

    data object DocumentDetails : Screen(
        name = "ISSUANCE_DOCUMENT_DETAILS",
        parameters = "?detailsType={detailsType}"
                + "&documentId={documentId}"
    )

    data object DocumentOffer : Screen(
        name = "ISSUANCE_DOCUMENT_OFFER",
        parameters = "?offerConfig={offerConfig}"
    )

    data object DocumentOfferCode : Screen(
        name = "ISSUANCE_DOCUMENT_OFFER_CODE",
        parameters = "?offerCodeUiConfig={offerCodeUiConfig}"
    )
}

sealed class PresentationScreens {
    data object PresentationRequest : Screen(
        name = "PRESENTATION_REQUEST",
        parameters = "?requestUriConfig={requestUriConfig}"
    )

    data object PresentationLoading : Screen(
        name = "PRESENTATION_LOADING",
    )
}

sealed class DashboardScreens {
    data object Dashboard : Screen(name = "DASHBOARD")
}

sealed class WebScreens {
    data object Main : Screen(
        name = "WEB_MAIN",
        parameters = "?path=dashboard",
        webPath = "dashboard"
    )

    data object AddDocument : Screen(
        name = "WEB_ADD_DOCUMENT",
        parameters = "?path=add-document",
        webPath = "add-document"
    )

    data object Activation : Screen(
        name = "WEB_ACTIVATION",
        parameters = "?path=activation",
        webPath = "activation"
    )

    data object SignFileShare : Screen(
        name = "WEB_SIGN_FILE_SHARE",
        parameters = "?filePath={filePath}",
        webPath = "sign"
    )

    data object ActivationSuccess : Screen(
        name = "WEB_ACTIVATION_SUCCESS",
        parameters = "?path=activation/2",
        webPath = "activation/2"
    )

    data object DeactivationSuccess : Screen(
        name = "WEB_DEACTIVATION_SUCCESS",
        parameters = "?path=activation/deactivated",
        webPath = "activation/deactivated"
    )

    data object Auth : Screen(
        name = "WEB_AUTH",
        parameters = "?path=onboarding",
        webPath = "onboarding"
    )

    data object AddPid : Screen(
        name = "WEB_PID",
        parameters = "?path=${ONBOARDING.SCREENS.AddPid}"
    )

    data object Email : Screen(
        name = "WEB_EMAIL",
        parameters = "?path=onboarding/3",
        webPath = "onboarding/3"
    )

    data object SMS : Screen(
        name = "WEB_PHONE",
        parameters = "?path=onboarding/1",
        webPath = "onboarding/1"
    )

    data object WELCOME : Screen(
        name = "WEB_WELCOME",
        parameters = "?path=onboarding/5",
        webPath = "onboarding/5"
    )

    data object LOADING : Screen(
        name = "WEB_LOADING",
        parameters = "?path=loading",
        webPath = "loading"
    )

    data object DocumentOffer : Screen(
        name = "WEB_DOCUMENT_OFFER",
        parameters = "?path=document-offer",
        webPath = "document-offer"
    )

    data object DocumentOfferCode : Screen(
        name = "WEB_DOCUMENT_OFFER_CODE",
        parameters = "?path=document-offer-code",
        webPath = "document-offer-code"
    )

    data object PresentationRequest : Screen(
        name = "PRESENTATION_REQUEST",
        parameters = "?path=document-presentation",
    )

    data object PresentationLoading : Screen(
        name = "PRESENTATION_LOADING",
        parameters = "?path=presentation-loading"
    )

    data object PresentationSuccess : Screen(
        name = "PRESENTATION_SUCCESS",
        parameters = "?path=presentation-success"
    )

    data object QrInvalid : Screen(
        name = "WEB_QR_INVALID",
        parameters = "?path=qr-invalid",
        webPath = "qr-invalid"
    )
}

sealed class ModuleRoute(val route: String) : NavigatableItem {
    data object StartupModule : ModuleRoute("STARTUP_MODULE")
    data object CommonModule : ModuleRoute("COMMON_MODULE")
    data object IssuanceModule : ModuleRoute("ISSUANCE_MODULE")
    data object PresentationModule : ModuleRoute("PRESENTATION_MODULE")
    data object WebModule : ModuleRoute("WEB_MODULE")
    data object DashboardModule : ModuleRoute("DASHBOARD_MODULE")
}
