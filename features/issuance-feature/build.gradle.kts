// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.issuancefeature"
}
dependencies {
    implementation(libs.androidx.browser)
    implementation(project(LibraryModule.NetworkLogic.path))
    implementation(project(LibraryModule.SignFeature.path))
    implementation(project(LibraryModule.CoreLogic.path))
}

moduleConfig {
    module = LibraryModule.IssuanceFeature
}
