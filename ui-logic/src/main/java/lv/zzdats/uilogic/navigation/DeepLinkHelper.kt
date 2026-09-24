// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.uilogic.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import lv.zzdats.businesslogic.util.safeLet
import lv.zzdats.corelogic.util.CoreActions
import lv.zzdats.uilogic.BuildConfig
import lv.zzdats.uilogic.DigimaksComponentActivity
import lv.zzdats.uilogic.extension.openUrl


fun <T> generateComposableArguments(arguments: Map<String, T>): String {
    if (arguments.isEmpty()) return ""
    return StringBuilder().apply {
        append("?")
        arguments.onEachIndexed { index, entry ->
            if (index > 0) {
                append("&")
            }
            append("${entry.key}=${entry.value}")
        }
    }.toString()
}

fun generateComposableDeepLinkUri(screen: Screen, arguments: String): Uri =
    generateComposableDeepLinkUri(screen.screenName, arguments)

fun generateComposableDeepLinkUri(screen: String, arguments: String): Uri =
    "${BuildConfig.DEEPLINK}/${screen}$arguments".toUri()

fun generateComposableNavigationLink(screen: Screen, arguments: String): String =
    generateComposableNavigationLink(screen.screenName, arguments)

fun generateComposableNavigationLink(screen: String, arguments: String): String =
    "${screen}$arguments"

fun generateNewTaskDeepLink(
    context: Context,
    screen: Screen,
    arguments: String = "",
    flags: Int = 0
): Intent =
    generateNewTaskDeepLink(context, screen.screenName, arguments, flags)

fun generateNewTaskDeepLink(
    context: Context,
    screen: String,
    arguments: String = "",
    flags: Int = 0
): Intent =
    Intent(
        Intent.ACTION_VIEW,
        generateComposableDeepLinkUri(screen, arguments),
        context,
        DigimaksComponentActivity::class.java
    ).apply {
        addFlags(flags)
    }

fun hasDeepLink(deepLinkUri: Uri?): DeepLinkAction? {
    return safeLet(
        deepLinkUri,
        deepLinkUri?.scheme
    ) { uri, scheme ->
        DeepLinkAction(link = uri, type = DeepLinkType.parse(scheme, uri.host))
    }
}

fun handleDeepLinkAction(
    navController: NavController,
    uri: Uri,
    arguments: String? = null
) {
    hasDeepLink(uri)?.let { action ->
        handleDeepLinkAction(navController, action, arguments)
    }
}

fun handleDeepLinkAction(
    navController: NavController,
    action: DeepLinkAction,
    arguments: String? = null
) {
    val screen: Screen

    when (action.type) {
        DeepLinkType.OPENID4VP -> {
            screen = PresentationScreens.PresentationRequest
        }

        DeepLinkType.CREDENTIAL_OFFER -> {
            screen = IssuanceScreens.DocumentOffer
        }

        DeepLinkType.ISSUANCE -> {
            notify(
                navController.context,
                CoreActions.VCI_RESUME_ACTION,
                bundleOf(Pair("uri", action.link.toString()))
            )
            return
        }

        DeepLinkType.EXTERNAL -> {
            navController.context.openUrl(action.link)
            return
        }

        DeepLinkType.DYNAMIC_PRESENTATION -> {
            notify(
                navController.context,
                CoreActions.VCI_DYNAMIC_PRESENTATION,
                bundleOf(Pair("uri", action.link.toString()))
            )
            return
        }

        DeepLinkType.SIGN_DOCUMENT -> {
            screen = WebScreens.SignFileShare
        }
    }

    val navigationLink = arguments?.let {
        generateComposableNavigationLink(
            screen = screen,
            arguments = arguments
        )
    } ?: screen.screenRoute

    navController.navigate(navigationLink) {
        popUpTo(screen.screenRoute) { inclusive = true }
    }
}

data class DeepLinkAction(val link: Uri, val type: DeepLinkType)
enum class DeepLinkType(val schemas: List<String>, val host: String? = null) {

    OPENID4VP(
        schemas = listOf(
            BuildConfig.OPENID4VP_SCHEME,
            BuildConfig.EUDI_OPENID4VP_SCHEME,
            BuildConfig.MDOC_OPENID4VP_SCHEME,
            BuildConfig.HAIP_OPENID4VP_SCHEME
        )
    ),
    CREDENTIAL_OFFER(
        schemas = listOf(
            BuildConfig.CREDENTIAL_OFFER_SCHEME,
            BuildConfig.CREDENTIAL_OFFER_HAIP_SCHEME
        )
    ),
    ISSUANCE(
        schemas = listOf(BuildConfig.ISSUE_AUTHORIZATION_SCHEME),
        host = BuildConfig.ISSUE_AUTHORIZATION_HOST
    ),
    EXTERNAL(
        emptyList()
    ),
    DYNAMIC_PRESENTATION(
        emptyList()
    ),
    SIGN_DOCUMENT(
        schemas = listOf(BuildConfig.SIGN_FILE_SHARE_SCHEME),
        host = BuildConfig.FILE_SHARE_HOST
    );

    companion object {
        fun parse(scheme: String, host: String? = null): DeepLinkType = when {

            OPENID4VP.schemas.contains(scheme) -> {
                OPENID4VP
            }

            CREDENTIAL_OFFER.schemas.contains(scheme) -> {
                CREDENTIAL_OFFER
            }

            ISSUANCE.schemas.contains(scheme) && host == ISSUANCE.host -> {
                ISSUANCE
            }

            SIGN_DOCUMENT.schemas.contains(scheme) && host == SIGN_DOCUMENT.host -> {
                SIGN_DOCUMENT
            }

            scheme == "content" -> {
                SIGN_DOCUMENT
            }

            else -> EXTERNAL
        }
    }
}

private fun notify(context: Context, action: String, bundle: Bundle? = null) {
    Intent().also { intent ->
        intent.action = action
        bundle?.let { intent.putExtras(it) }
        context.sendBroadcast(intent)
    }
}