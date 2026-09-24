// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.issuancefeature.di

import lv.zzdats.authlogic.service.AuthService
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.commonfeature.features.user.onboarding.OnboardingInteractor
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.security.SecureAreaController
import lv.zzdats.issuancefeature.ui.document.add.AddDocumentInteractor
import lv.zzdats.issuancefeature.ui.document.add.AddDocumentInteractorImpl
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractor
import lv.zzdats.issuancefeature.ui.document.details.DocumentDetailsInteractorImpl
import lv.zzdats.issuancefeature.ui.document.offer.DocumentOfferInteractor
import lv.zzdats.issuancefeature.ui.document.offer.DocumentOfferInteractorImpl
import lv.zzdats.issuancefeature.ui.success.SuccessInteractor
import lv.zzdats.issuancefeature.ui.success.SuccessInteractorImpl
import lv.zzdats.networklogic.api.wallet.WalletApiClient
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.BookmarkDao
import lv.zzdats.uilogic.serializer.UiSerializer
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.zzdats.issuancefeature")
class IssuanceModule

@Factory
fun provideAddDocumentInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
    walletApiClient: WalletApiClient,
    authService: AuthService
): AddDocumentInteractor =
    AddDocumentInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
        walletApiClient,
        authService
    )

@Factory
fun provideSuccessInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController
): SuccessInteractor = SuccessInteractorImpl(resourceProvider, walletCoreDocumentsController)

@Factory
fun provideDocumentOfferInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    resourceProvider: ResourceProvider,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    uiSerializer: UiSerializer,
    authService: AuthService,
    walletApiClient: WalletApiClient,
    secureAreaController: SecureAreaController,
    onboardingInteractor: OnboardingInteractor,
    logController: LogController
): DocumentOfferInteractor =
    DocumentOfferInteractorImpl(
        walletCoreDocumentsController,
        deviceAuthenticationInteractor,
        resourceProvider,
        uiSerializer,
        authService,
        walletApiClient,
        secureAreaController,
        onboardingInteractor,
        logController
    )

@Factory
fun provideDocumentDetailsInteractor(
    walletCoreDocumentsController: WalletCoreDocumentsController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor,
    resourceProvider: ResourceProvider,
    bookmarkDao: BookmarkDao
): DocumentDetailsInteractor =
    DocumentDetailsInteractorImpl(
        walletCoreDocumentsController = walletCoreDocumentsController,
        deviceAuthenticationInteractor = deviceAuthenticationInteractor,
        bookmarkDao = bookmarkDao,
        resourceProvider = resourceProvider
    )
