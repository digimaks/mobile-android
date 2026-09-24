// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.resourceslogic.bridge


object LX {
    const val LX_EMBED_RESPONSE = "lx-embed-response"
}

object SETTINGS {
    const val BRIDGE_NAME = "settings"

    const val ENABLE_BIOMETRICS = "enableBiometrics"
    const val GET_BIOMETRIC_AVAILABILITY = "getBiometricAvailability"
    const val SET_THEME = "setTheme"
    const val SET_LANGUAGE = "setLanguage"
    const val DELETE_WALLET = "deleteWallet"
}

object ISSUANCE {
    const val BRIDGE_NAME = "issuance"

    const val GET_SAMPLE_DOCUMENTS = "getSampleDocuments"
    const val DELETE_SAMPLE_DOCUMENTS = "deleteSampleDocuments"
    const val GET_DOCUMENT_OPTIONS = "getDocumentOptions"
    const val GET_PID_OPTIONS = "getPidOptions"
    const val ISSUE_DOCUMENT = "issueDocument"
    const val RESUME_ISSUANCE = "resumeIssuance"
    const val GET_PID_DETAILS = "getPidDetails"
    const val SCAN_QR_CODE = "scanQrCode"
    const val ISSUE_DOCUMENT_OFFER = "issueDocumentOffer"
    const val RESOLVE_DOCUMENT_OFFER = "resolveDocumentOffer"
    const val GET_OFFER_CODE_DATA = "getOfferCodeData"
    const val SELECT_USER_SIGNATURES = "selectUserSignatures"
    const val GET_USER_SIGNATURE_OPTIONS = "getUserSignatureOptions"
    const val LAUNCH_SEB_ACTIVITY = "launchSEB"

    object SCREENS {
        const val PID_SUCCESS = "pid-success"
        const val DOCUMENT_SUCCESS = "document-success"
        const val DOCUMENT_DEFERRED_SUCCESS = "document-deferred-success"
    }

    object ERRORS {
        const val GET_DOCUMENT_OPTIONS_FAILED = "GET_DOCUMENT_OPTIONS_FAILED"
    }
}

object SIGN {
    const val BRIDGE_NAME = "sign"

    const val PICK_FILES = "pickFiles"
    const val GET_SIGNING_METHODS = "getSigningMethods"
    const val SIGN_DOCUMENT = "signDocument"
    const val ESEAL_DOCUMENT = "eSealDocument"
    const val DOWNLOAD_DOCUMENT = "downloadSignedDocument"
    const val SHARE_DOCUMENT = "shareSignedDocument"
    const val CLOSE_SESSION = "closeSession"
    const val GET_SHARED_FILE = "getSharedFile"
    const val OPEN_FILE = "openFile"
}

object ONBOARDING {
    const val BRIDGE_NAME = "onboarding"

    const val INITIATE_EPARAKSTS = "initiateEParaksts"
    const val INITIATE_SMART_ID = "initiateSmartID"
    const val ACTIVATE_WALLET = "activateWallet"
    const val INITIALISE_WALLET = "initialiseWallet"
    const val INITIATE_BIOMETRICS = "initiateBiometrics"
    const val OPEN_TERMS = "openTerms"

    object SCREENS {
        const val LOADING = "loading"
        const val AddPid = "dashboard" // TODO:
    }
}

object DASHBOARD {
    const val BRIDGE_NAME = "dashboard"

    const val GET_DOCUMENTS = "getDocuments"
    const val GET_DOCUMENT_DETAILS = "getDocumentDetails"
    const val DELETE_DOCUMENT = "deleteDocument"
    const val REISSUE_DOCUMENT = "reIssueDocument"
    const val SET_DOCUMENT_FAVORITE = "setDocumentFavorite"

    object SCREENS {
        const val MAIN = "dashboard"
    }
}

object PRESENTATION {
    const val BRIDGE_NAME = "presentation"

    const val GET_REQUEST_DOCUMENTS = "getRequestDocuments"
    const val CONFIRM_REQUEST = "confirmRequest"
    const val START_PRESENTATION = "scanQrCode"
    const val CANCEL_REQUEST = "presentationCanceled"
    const val SET_VENDOR_PRESENTATION_PREFERENCE = "setVendorPresentationPreference"

    object SCREENS {
        const val PRESENTATION_LOADING = "presentation-loading"
    }
}

object TRANSACTIONS {
    const val BRIDGE_NAME = "transactions"

    const val GET_TRANSACTIONS = "getTransactions"

    object SCREENS {
        const val MAIN = "transactionsHistory"
    }
}

object PROXIMITY {
    const val BRIDGE_NAME = "proximityBridge"

    const val START_QR_ENGAGEMENT = "startQrEngagement"
    const val GET_REQUEST_DOCUMENTS = "getRequestDocuments"
    const val UPDATE_REQUESTED_DOCUMENTS = "updateRequestedDocuments"
    const val SEND_REQUESTED_DOCUMENTS = "sendRequestedDocuments"
    const val OBSERVE_RESPONSE = "observeResponse"
    const val SET_CONFIG = "setConfig"
    const val CANCEL_TRANSFER = "cancelTransfer"
    const val STOP_PRESENTATION = "stopPresentation"
    const val SCAN_QR_CODE = "scanQrCode"
    const val START_PRESENTATION = "startPresentation"
    const val GET_SUCCESS_DATA = "getSuccessData"
    const val TOGGLE_NFC = "toggleNfc"

    object SCREENS {
        const val QR = "proximity/qr"
        const val REQUEST = "proximity/request"
        const val LOADING = "proximity/loading"
        const val SUCCESS = "proximity/success"
    }

    object ERRORS {
        const val QR_GENERATION_FAILED = "qr_generation_failed"
        const val CONNECTION_FAILED = "connection_failed"
        const val AUTHENTICATION_FAILED = "authentication_failed"
        const val TRANSFER_FAILED = "transfer_failed"
    }
}
