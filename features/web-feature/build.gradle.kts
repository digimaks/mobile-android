// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.webfeature"
}

dependencies {
    implementation(project(LibraryModule.WebBridge.path))
    implementation(project(LibraryModule.CommonFeature.path))
    implementation(project(LibraryModule.DashboardFeature.path))
    implementation(project(LibraryModule.IssuanceFeature.path))
    implementation(project(LibraryModule.NetworkLogic.path))
    implementation(project(LibraryModule.StartupFeature.path))
    implementation(project(LibraryModule.PresentationFeature.path))
    implementation(project(LibraryModule.TransactionsFeature.path))
    implementation(project(LibraryModule.SignFeature.path))
}

moduleConfig {
    module = LibraryModule.WebFeature
}