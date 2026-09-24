// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.signfeature"
}

dependencies {
    implementation(project(LibraryModule.NetworkLogic.path))
    api(libs.upokecenter.cbor)
    implementation(libs.androidx.browser)
}

moduleConfig {
    module = LibraryModule.SignFeature
}