// SPDX-License-Identifier: EUPL-1.2

package lv.zzdats.proximityfeature.di

import lv.zzdats.proximityfeature.interactor.ProximityLoadingInteractor
import lv.zzdats.proximityfeature.interactor.ProximityLoadingInteractorImpl
import lv.zzdats.proximityfeature.interactor.ProximityQRInteractor
import lv.zzdats.proximityfeature.interactor.ProximityQRInteractorImpl
import lv.zzdats.proximityfeature.interactor.ProximityRequestInteractor
import lv.zzdats.proximityfeature.interactor.ProximityRequestInteractorImpl
import lv.zzdats.proximityfeature.interactor.ProximitySuccessInteractor
import lv.zzdats.proximityfeature.interactor.ProximitySuccessInteractorImpl
import lv.zzdats.businesslogic.provider.UuidProvider
import lv.zzdats.commonfeature.features.auth.DeviceAuthenticationInteractor
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.corelogic.controller.WalletCorePresentationController
import lv.zzdats.corelogic.di.PRESENTATION_SCOPE_ID
import lv.zzdats.resourceslogic.provider.ResourceProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.ScopeId

@Module
@ComponentScan("lv.zzdats.proximityfeature")
class FeatureProximityModule

@Factory
fun provideProximityQRInteractor(
    resourceProvider: ResourceProvider,
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController
): ProximityQRInteractor =
    ProximityQRInteractorImpl(resourceProvider, walletCorePresentationController)

@Factory
fun provideProximityRequestInteractor(
    resourceProvider: ResourceProvider,
    uuidProvider: UuidProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController
): ProximityRequestInteractor =
    ProximityRequestInteractorImpl(
        resourceProvider,
        uuidProvider,
        walletCorePresentationController,
        walletCoreDocumentsController
    )

@Factory
fun provideProximityLoadingInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
    deviceAuthenticationInteractor: DeviceAuthenticationInteractor
): ProximityLoadingInteractor =
    ProximityLoadingInteractorImpl(walletCorePresentationController, deviceAuthenticationInteractor)

@Factory
fun provideProximitySuccessInteractor(
    @ScopeId(name = PRESENTATION_SCOPE_ID) walletCorePresentationController: WalletCorePresentationController,
): ProximitySuccessInteractor {
    return ProximitySuccessInteractorImpl(
        walletCorePresentationController,
    )
}