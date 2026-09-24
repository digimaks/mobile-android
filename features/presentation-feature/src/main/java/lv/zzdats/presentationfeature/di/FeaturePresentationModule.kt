// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.presentationfeature.di

import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCorePresentationController
import lv.zzdats.corelogic.di.PRESENTATION_SCOPE_ID
import lv.zzdats.presentationfeature.interactor.PresentationLoadingInteractor
import lv.zzdats.presentationfeature.interactor.PresentationLoadingInteractorImpl
import lv.zzdats.presentationfeature.interactor.PresentationRequestInteractor
import lv.zzdats.presentationfeature.interactor.PresentationRequestInteractorImpl
import lv.zzdats.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.ScopeId

@Module
@ComponentScan("lv.zzdats.presentationfeature")
class FeaturePresentationModule

@Factory
fun providePresentationRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController
): PresentationRequestInteractor {
    return PresentationRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCorePresentationController,
        walletCoreDocumentsController
    )
}

@Factory
fun providePresentationLoadingInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor
): PresentationLoadingInteractor {
    return PresentationLoadingInteractorImpl(
        walletCorePresentationController,
        deviceAuthenticationInteractor
    )
}