// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.startupfeature.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import lv.zzdats.resourceslogic.R
import lv.zzdats.startupfeature.update.InAppUpdateHelper
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.content.ContentScreen
import lv.zzdats.uilogic.components.content.ContentTitle
import lv.zzdats.uilogic.components.content.ScreenNavigateAction
import lv.zzdats.uilogic.components.content.LoadingType
import lv.zzdats.uilogic.components.utils.SPACING_LARGE
import lv.zzdats.uilogic.components.utils.VSpacer
import lv.zzdats.uilogic.components.wrap.WrapIcon
import lv.zzdats.uilogic.components.wrap.WrapPrimaryButton
import lv.zzdats.uilogic.components.wrap.WrapSecondaryButton
import lv.zzdats.uilogic.extension.finish
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.play.core.install.model.AppUpdateType

@Composable
fun ForceUpdateScreen(
    storeUrl: String,
    inAppUpdateHelper: InAppUpdateHelper
) {
    val context = LocalContext.current
    val decodedStoreUrl = Uri.decode(storeUrl)
    val coroutineScope = rememberCoroutineScope()

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
                    iconData = AppIcons.Warning,
                    modifier = Modifier.size(96.dp),
                    customTint = MaterialTheme.colorScheme.error
                )

                VSpacer.ExtraLarge()

                ContentTitle(
                    title = stringResource(id = R.string.force_update_title),
                    subtitle = stringResource(id = R.string.force_update_description),
                    titleStyle = MaterialTheme.typography.headlineMedium,
                    subTitleStyle = MaterialTheme.typography.bodyLarge
                )
            }

            Column(
                modifier = Modifier.padding(bottom = SPACING_LARGE.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WrapPrimaryButton(
                    onClick = {
                        coroutineScope.launch {
                            val started = inAppUpdateHelper.startUpdate(
                                activity = (context as? android.app.Activity) ?: return@launch,
                                type = AppUpdateType.IMMEDIATE
                            )
                            if (!started) {
                                openStorePage(
                                    context = context,
                                    url = decodedStoreUrl,
                                    fallbackPackage = context.packageName
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.force_update_cta))
                }

                VSpacer.Medium()

                WrapSecondaryButton(
                    onClick = { context.finish() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.force_update_close))
                }
            }
        }
    }
}

private fun openStorePage(context: Context, url: String, fallbackPackage: String) {
    val primaryUri = url.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        ?: Uri.parse("market://details?id=$fallbackPackage")

    val fallbackUri = Uri.parse("https://play.google.com/store/apps/details?id=$fallbackPackage")

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
