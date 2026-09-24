// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.commonfeature.features.qr_scan

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import lv.zzdats.commonfeature.features.qr_scan.component.QrCodeAnalyzer
import lv.zzdats.resourceslogic.R
import lv.zzdats.uilogic.components.AppIcons
import lv.zzdats.uilogic.components.ErrorInfo
import lv.zzdats.uilogic.components.content.ContentScreen
import lv.zzdats.uilogic.components.content.ScreenNavigateAction
import lv.zzdats.uilogic.components.wrap.WrapIconButton
import lv.zzdats.uilogic.extension.openAppSettings
import lv.zzdats.uilogic.extension.throttledClickable
import lv.zzdats.uilogic.navigation.CommonScreens

@Composable
fun QrScanScreen(
    navController: NavController,
    viewModel: QrScanViewModel
) {
    val state = viewModel.viewState.value
    val context = LocalContext.current

    val systemUiController = rememberSystemUiController()
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    
    LaunchedEffect(Unit) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false,
            isNavigationBarContrastEnforced = false
        )
    }

    BackHandler(enabled = true) {
        viewModel.setEvent(Event.GoBack)
    }

    ContentScreen(
        isLoading = false,
        navigatableAction = ScreenNavigateAction.CANCELABLE,
        topBar = {},
        bottomBar = {},
        onBack = { viewModel.setEvent(Event.GoBack) }
    ) { paddingValues ->
        Content(
            state = state,
            effectFlow = viewModel.effect,
            onEventSend = { viewModel.setEvent(it) },
            onNavigationRequested = { navigationEffect ->
                handleNavigationEffect(context, navigationEffect, navController)
            },
            paddingValues = paddingValues,
            onBack = { viewModel.setEvent(Event.GoBack) },
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            systemUiController.setSystemBarsColor(
                color = backgroundColor,
                darkIcons = !isDark,
                isNavigationBarContrastEnforced = true
            )
        }
    }
}

@Composable
private fun Content(
    state: State,
    effectFlow: Flow<Effect>,
    onEventSend: (Event) -> Unit,
    onNavigationRequested: (navigationEffect: Effect.Navigation) -> Unit,
    paddingValues: PaddingValues,
    onBack: (() -> Unit),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp)
    ) {
        OpenCamera(
            hasCameraPermission = state.hasCameraPermission,
            shouldShowPermissionRational = state.shouldShowPermissionRational,
            onEventSend = onEventSend,
            onQrScanned = { qrCode ->
                onEventSend(Event.OnQrScanned(resultQr = qrCode))
            }
        )

        Box(
            modifier = Modifier
                .systemBarsPadding()
                .padding(
                    top = 2.dp,
                    end = 4.dp
                )
                .size(48.dp)
                .align(Alignment.TopEnd)
                .zIndex(1f)
        ) {
            WrapIconButton(
                iconData = AppIcons.Close,
                customTint = Color.White,
                onClick = onBack,
                modifier = Modifier.fillMaxSize()
            )
        }
    }


    LaunchedEffect(Unit) {
        effectFlow.onEach { effect ->
            when (effect) {
                is Effect.Navigation -> onNavigationRequested(effect)
            }
        }.collect()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun OpenCamera(
    hasCameraPermission: Boolean,
    shouldShowPermissionRational: Boolean,
    onEventSend: (Event) -> Unit,
    onQrScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val permissionState = rememberPermissionState(permission = android.Manifest.permission.CAMERA)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            permissionState.status.isGranted -> {
                onEventSend(Event.CameraAccessGranted)
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        preview.surfaceProvider = previewView.surfaceProvider

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            QrCodeAnalyzer { result -> onQrScanned(result) }
                        )

                        try {
                            cameraProviderFuture.get().bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        previewView
                    }
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameWidth = size.width * 0.7f
                    val frameHeight = size.height * 0.3f
                    val left = (size.width - frameWidth) / 2
                    val top = (size.height - frameHeight) / 2
                    val right = left + frameWidth
                    val bottom = top + frameHeight
                    val cornerSize = 40f
                    val strokeWidth = 12f
                    val white = Color.White

                    // Draw white corner brackets
                    // Top-left corner
                    drawLine(
                        white,
                        Offset(left, top),
                        Offset(left + cornerSize, top),
                        strokeWidth,
                        StrokeCap.Round
                    )
                    drawLine(
                        white,
                        Offset(left, top),
                        Offset(left, top + cornerSize),
                        strokeWidth,
                        StrokeCap.Round
                    )

                    // Top-right corner
                    drawLine(
                        white,
                        Offset(right, top),
                        Offset(right - cornerSize, top),
                        strokeWidth,
                        StrokeCap.Round
                    )
                    drawLine(
                        white,
                        Offset(right, top),
                        Offset(right, top + cornerSize),
                        strokeWidth,
                        StrokeCap.Round
                    )

                    // Bottom-left corner
                    drawLine(
                        white,
                        Offset(left, bottom),
                        Offset(left + cornerSize, bottom),
                        strokeWidth,
                        StrokeCap.Round
                    )
                    drawLine(
                        white,
                        Offset(left, bottom),
                        Offset(left, bottom - cornerSize),
                        strokeWidth,
                        StrokeCap.Round
                    )

                    // Bottom-right corner
                    drawLine(
                        white,
                        Offset(right, bottom),
                        Offset(right - cornerSize, bottom),
                        strokeWidth,
                        StrokeCap.Round
                    )
                    drawLine(
                        white,
                        Offset(right, bottom),
                        Offset(right, bottom - cornerSize),
                        strokeWidth,
                        StrokeCap.Round
                    )
                }
            }
            permissionState.status.shouldShowRationale -> {
                onEventSend(Event.ShowPermissionRational)
                ErrorInfo(
                    modifier = Modifier.throttledClickable { onEventSend(Event.GoToAppSettings) },
                    informativeText = stringResource(id = R.string.qr_scan_permission_not_granted),
                )
            }
            else -> {
                LaunchedEffect(Unit) {
                    permissionState.launchPermissionRequest()
                }
                ErrorInfo(
                    modifier = Modifier.throttledClickable { permissionState.launchPermissionRequest() },
                    informativeText = stringResource(id = R.string.qr_scan_grant_permission),
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun handleNavigationEffect(
    context: Context,
    navigationEffect: Effect.Navigation,
    navController: NavController
) {
    when (navigationEffect) {
        is Effect.Navigation.SwitchScreen -> {
            navController.navigate(navigationEffect.screenRoute) {
                popUpTo(CommonScreens.QrScan.screenRoute) {
                    inclusive = true
                }
            }
        }

        is Effect.Navigation.Pop -> {
            navController.popBackStack()
        }

        is Effect.Navigation.GoToAppSettings -> context.openAppSettings()
    }
}
