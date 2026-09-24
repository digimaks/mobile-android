// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.webkit.WebViewCompat
import lv.zzdats.corelogic.security.SecurityReason
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.content.*
import lv.zzdats.uilogic.components.utils.*
import lv.zzdats.uilogic.components.wrap.*
import lv.zzdats.resourceslogic.R

@Composable
fun SecurityErrorScreen(
    reason: String,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val securityReason = SecurityReason.entries.find { it.name == reason }
    val isWebViewOutdated = securityReason == SecurityReason.WEBVIEW_TOO_OLD
    val needsLegacyBiometric = securityReason == SecurityReason.LEGACY_BIOMETRIC_REQUIRED

    ContentScreen(
        navigatableAction = ScreenNavigateAction.NONE,
        contentErrorConfig = null,
        loadingType = LoadingType.NONE
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SPACING_LARGE.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                WrapIcon(
                    iconData = AppIcons.Error,
                    customTint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(96.dp)
                )

                VSpacer.ExtraLarge()

                ContentTitle(
                    title = stringResource(id = R.string.security_error_title),
                    subtitle = getErrorDescription(reason),
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    subTitleStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Column(
                modifier = Modifier.padding(bottom = SPACING_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isWebViewOutdated || needsLegacyBiometric) {
                    WrapPrimaryButton(
                        onClick = {
                            if (isWebViewOutdated) {
                                openWebViewProviderStore(context)
                            } else {
                                openBiometricSettings(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                id = if (isWebViewOutdated) {
                                    R.string.security_error_update_webview
                                } else {
                                    R.string.security_error_open_security_settings
                                }
                            )
                        )
                    }

                    VSpacer.Medium()

                    WrapSecondaryButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.security_error_exit))
                    }
                } else {
                    WrapPrimaryButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.security_error_exit))
                    }
                }

                VSpacer.Medium()

                Text(
                    text = stringResource(id = R.string.security_error_support),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun getErrorDescription(reason: String): String {
    return when (SecurityReason.entries.find { it.name == reason }) {
        SecurityReason.NO_SCREEN_LOCK -> stringResource(
            id = R.string.security_error_no_screen_lock,
            "SEC_002"
        )
        SecurityReason.WEBVIEW_TOO_OLD -> stringResource(
            id = R.string.security_error_webview_outdated,
            SecurityReason.WEBVIEW_TOO_OLD.code
        )
        SecurityReason.LEGACY_BIOMETRIC_REQUIRED -> stringResource(
            id = R.string.security_error_legacy_biometric_required,
            SecurityReason.LEGACY_BIOMETRIC_REQUIRED.code
        )
        else -> stringResource(
            id = R.string.security_error_generic,
            reason
        )
    }
}

private fun openWebViewProviderStore(context: android.content.Context) {
    val providerPackage = WebViewCompat.getCurrentWebViewPackage(context)?.packageName
        ?: "com.google.android.webview"

    val primaryUri = Uri.parse("market://details?id=$providerPackage")
    val fallbackUri = Uri.parse("https://play.google.com/store/apps/details?id=$providerPackage")

    val intents = listOf(primaryUri, fallbackUri).map {
        Intent(Intent.ACTION_VIEW, it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    intents.forEach {
        try {
            context.startActivity(it)
            return
        } catch (_: Exception) {
        }
    }
}

private fun openBiometricSettings(context: android.content.Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}
