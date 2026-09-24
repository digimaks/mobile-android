// SPDX-License-Identifier: EUPL-1.2

/*
 * Copyright (c) 2023 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work
 * except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific language
 * governing permissions and limitations under the Licence.
 */

package lv.zzdats.dashboardfeature.di

import lv.zzdats.dashboardfeature.interactor.DashboardInteractor
import lv.zzdats.dashboardfeature.interactor.DashboardInteractorImpl
import lv.zzdats.businesslogic.config.ConfigLogic
import lv.zzdats.businesslogic.controller.log.LogController
import lv.zzdats.corelogic.config.WalletConfig
import lv.zzdats.corelogic.controller.WalletCoreDocumentsController
import lv.zzdats.resourceslogic.provider.ResourceProvider
import lv.zzdats.storagelogic.dao.BookmarkDao
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
@ComponentScan("lv.zzdats.dashboardfeature")
class FeatureDashboardModule

@Factory
fun provideDashboardInteractor(
    resourceProvider: ResourceProvider,
    walletCoreDocumentsController: WalletCoreDocumentsController,
    walletCoreConfig: WalletConfig,
    configLogic: ConfigLogic,
    logController: LogController,
    bookmarkDao: BookmarkDao
): DashboardInteractor =
    DashboardInteractorImpl(
        resourceProvider,
        walletCoreDocumentsController,
        walletCoreConfig,
        configLogic,
        logController,
        bookmarkDao
    )