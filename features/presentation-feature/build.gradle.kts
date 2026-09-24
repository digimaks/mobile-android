// SPDX-License-Identifier: EUPL-1.2

import project.convention.logic.config.LibraryModule

plugins {
    id("project.android.feature")
}

android {
    namespace = "lv.zzdats.presentationfeature"
}
dependencies {
    implementation(project(":features:issuance-feature"))
}

moduleConfig {
    module = LibraryModule.PresentationFeature
}

dependencies {
    implementation(project(LibraryModule.NetworkLogic.path))
}